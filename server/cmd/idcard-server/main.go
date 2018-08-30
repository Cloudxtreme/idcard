package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"os/user"
	"strconv"
	"time"

	_ "github.com/jackc/pgx/stdlib"
	"github.com/riking/idcard/server/card42"
)

// Flags

var (
	configFileLocation    = flag.String("config", "./card42-config.json", "Server configuration file")
	operationWriteService = flag.Bool("write-systemd", false, "Write out a template SystemD service file and exit.")
	operationWriteConfig  = flag.Bool("write-config", false, "Write out a template configuration file and exit.")
)

const ExitCodeBadConfig = 10

func main() {
	flag.Parse()

	// Alternate modes of operation
	if *operationWriteService {
		createServiceFile()
		return
	}
	if *operationWriteConfig {
		createConfigFile()
		return
	}

	// Read configuration file
	_, err := os.Stat(*configFileLocation)
	if os.IsNotExist(err) {
		fmt.Println("Config file not found, creating blank config.")
		createConfigFile()
		return
	}
	err = card42.LoadServerConfig(*configFileLocation)
	if err != nil {
		fmt.Fprintf(os.Stderr, "%+v\n", err)
		os.Exit(ExitCodeBadConfig)
	}

	// Set up HTTP server
	s := &http.Server{
		Addr:              card42.GetServerConfig().HttpListen,
		ReadHeaderTimeout: 20 * time.Second,
		WriteTimeout:      20 * time.Second,
		IdleTimeout:       5 * time.Minute,
		MaxHeaderBytes:    1 << 20,
	}
	card42.SetUpServer(s)

}

func createConfigFile() {
	c := card42.ServerConfigFile{
		IntraComment: card42.IntraComment,
		HttpComment1: card42.HttpComment1,
		HttpComment2: card42.HttpComment2,
		SQLComment1:  card42.SQLComment1,
		SQLComment2:  card42.SQLComment2,
		SQLComment3:  card42.SQLComment3,
		KeysFile:     "./keys.json",
		SQLDriver:    "postgres",
		SQLDBName:    "card42",
	}

	by, err := json.MarshalIndent(c, "", "  ")
	fatalErr(err, "write example config", "marshal json")

	err = ioutil.WriteFile(*configFileLocation, by, 0600)
	fatalErr(err, "write example config", "write file")

	if os.Getuid() == 0 {
		fmt.Println("notice: running as root, attempting to chown config file to service user")

		subr := func() {
			u, err := user.Lookup("card42")
			if _, ok := err.(user.UnknownUserError); ok {
				fmt.Println("warning: service account does not exist, the config file may be insecure.")
				return
			}
			uid, err := strconv.Atoi(u.Uid)
			if err != nil {
				fmt.Println("notice: not on Unix, don't know how to chown file to u:" + u.Uid)
				return
			}
			gid, err := strconv.Atoi(u.Gid)
			if err != nil {
				fmt.Println("notice: not on Unix, don't know how to chown file to g:" + u.Gid)
				return
			}

			err = os.Chown(*configFileLocation, uid, gid)
			if err != nil {
				fmt.Println("warning: failed to chown file:", err)
			}
			return
		}
		subr()
	}
}

const SystemdServiceFile = `[Unit]
Description=Card42 API Server
Requires=nginx.service

[Service]
ExecStart=%s
Type=simple
Restart=on-failure
RestartPreventExitStatus=9,10
KillSignal=SIGINT
StandardOutput=journal
StandardError=journal
WorkingDirectory=%s
User=card42

[Install]
WantedBy=multi-user.target
`

func createServiceFile() {
	filename := "./card42.service"
	f, err := os.Create(filename)
	fatalErr(err, "make example service file", "create file")

	binPath := os.Args[0]
	cwd, err := os.Getwd()
	fatalErr(err, "make example service file", "get working directory")

	_, err = fmt.Fprintf(f, SystemdServiceFile, binPath, cwd)
	fatalErr(err, "make example service file", "write to file")

	fmt.Println("Wrote systemd service file to", filename)
}

func fatalErr(err error, goal, op string) {
	if err == nil {
		return
	}
	fmt.Fprintf(os.Stderr, "Could not %s:\n%s: %v\n", goal, op, err)
	os.Exit(1)
}

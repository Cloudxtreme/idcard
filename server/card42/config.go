package card42

import (
	"encoding/json"
	"os"

	"github.com/pkg/errors"
)

type ServerConfigFile struct {
	KeysFile string `json:"KeysFile"`

	HttpComment1 string `json:"HttpBaseURL//"`
	HttpBaseURL  string `json:"HttpBaseURL"`
	HttpComment2 string `json:"HttpListen//"`
	HttpListen   string `json:"HttpListen"`

	IntraComment   string `json:"IntraClient//"`
	IntraClientID  string `json:"IntraClientID"`
	IntraClientSec string `json:"IntraClientSec"`

	SQLComment1 string `json:"SQLConnect//"`
	SQLConnect  string `json:"SQLConnect"`
	SQLComment2 string `json:"SQLDriver//"`
	SQLDriver   string `json:"SQLDriver"`
	SQLComment3 string `json:"SQLDBName//"`
	SQLDBName   string `json:"SQLDBName"`
}

var globalConfig ServerConfigFile

const IntraComment = "Intranet login oauth client id/secret."
const HttpComment1 = "The HttpBaseURL is the URL of this server as seen by the outside world, optionally with a path component that does NOT end with a slash. Example: [[https://card42-test.riking.org/subfolder]]"
const HttpComment2 = "The HttpListen is the IP/port that the Go HTTP server will listen on. It's assumed that this runs behind a reverse proxy like nginx that handles HTTPS and h2. Example: [[127.0.0.1:8007]]"
const SQLComment1 = "A connection string to connect to the database. Example of a DSN: [[user=card42 password=abcdef12345shouldberandom host=pq.db.42.us.org dbname=card42srv sslmode=verify-full sslrootcert=/etc/ssl/corpca.pem]]"
const SQLComment2 = "Golang name of the SQL driver. Example: [[postgres]]"
const SQLComment3 = "Database name for use by the application code. Should be the same as the table name in SQLConnect. Example: [[card42srv]]"

func LoadServerConfig(path string) error {
	f, err := os.Open(path)
	if err != nil {
		return errors.Wrap(err, "Could not read config file")
	}
	var conf ServerConfigFile
	err = json.NewDecoder(f).Decode(&conf)
	if err != nil {
		return errors.Wrap(err, "Could not read config file")
	}
	if conf.HttpListen == "" {
		return errors.Errorf("Config file is not set up, please fill it before starting (HttpListen is blank)")
	}
	globalConfig = conf
	return nil
}

func GetServerConfig() *ServerConfigFile {
	return &globalConfig
}

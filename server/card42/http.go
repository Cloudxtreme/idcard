package card42

import (
	"fmt"
	"net/http"
)

func SetUpServer(s *http.Server) {
	mux := http.NewServeMux()
	s.Handler = mux

	mux.HandleFunc("/whoami", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain")
		w.Header().Set("X-Content-Type-Options", "nosniff")
		w.Header().Set("Test-Header", "present")

		fmt.Fprintln(w, "Hello, HTTP!")
		if r.Header.Get("Test-Header") != "" {
			fmt.Fprintln(w, "got Test-Header")
		}
		fmt.Fprintln(w, "You are:", r.Header.Get("User-Agent"))
	})
}

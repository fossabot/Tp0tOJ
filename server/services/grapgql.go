package services

import (
	"context"
	"crypto/rand"
	_ "embed"
	"encoding/binary"
	"github.com/gorilla/csrf"
	"github.com/gorilla/mux"
	"github.com/graph-gophers/graphql-go"
	"github.com/graph-gophers/graphql-go/relay"
	"github.com/kataras/go-sessions/v3"
	"io/fs"
	"log"
	unsafeRand "math/rand"
	"net/http"
	"os"
	"path/filepath"
	"server/services/admin"
	"server/services/sse"
	"server/services/user"
	"server/utils/kick"
	"strings"
	"time"
)

type Resolver struct {
	admin.AdminMutationResolver
	admin.AdminQueryResolver
	user.MutationResolver
	user.QueryResolver
}

//go:embed schema.graphql
var schemaStr string
var CSRFMiddleware func(http.Handler) http.Handler

func getIP(r *http.Request) string {
	forward := r.Header.Get("X-FORWARDED-FOR")
	if forward == "" {
		return r.RemoteAddr
	}
	return strings.TrimSpace(strings.Split(forward, ",")[0])
}

func init() {
	muxRouter := mux.NewRouter()
	seed := make([]byte, 8)
	_, err := rand.Read(seed)
	if err != nil {
		log.Panicln("can not generate rand", err)
		return
	}

	//Protect CSRF
	unsafeRand.Seed(int64(binary.BigEndian.Uint64(seed)))
	token := make([]byte, 32)
	_, err = unsafeRand.Read(token)
	if err != nil {
		log.Panicln("can not generate rand", err)
		return
	}
	CSRFMiddleware = csrf.Protect(token)

	sessionManager := sessions.New(sessions.Config{
		// Cookie string, the session's client cookie name, for example: "mysessionid"
		//
		// Defaults to "gosessionid"
		Cookie: "session",
		// it's time.Duration, from the time cookie is created, how long it can be alive?
		// 0 means no expire.
		// -1 means expire when browser closes
		// or set a value, like 2 hours:
		Expires: time.Hour * 1,
		// if you want to invalid cookies on different subdomains
		// of the same host, then enable it
		DisableSubdomainPersistence: false,
		// want to be crazy safe? Take a look at the "securecookie" example folder.
	})

	schema := graphql.MustParseSchema(schemaStr, &Resolver{}, graphql.UseFieldResolvers())
	//http.Handle("/query", &relay.Handler{Schema: schema})
	graphqlHandle := &relay.Handler{Schema: schema}
	muxRouter.HandleFunc("/graphql", func(w http.ResponseWriter, r *http.Request) {
		session := sessionManager.Start(w, r)
		ctx := r.Context()
		ctx = context.WithValue(ctx, "session", session)
		ctx = context.WithValue(ctx, "ip", getIP(r))
		graphqlHandle.ServeHTTP(w, r.WithContext(ctx))
	})
	muxRouter.HandleFunc("/writeup", func(w http.ResponseWriter, r *http.Request) {
		session := sessionManager.Start(w, r)
		isLogin := session.Get("isLogin")
		if isLogin == nil || !*isLogin.(*bool) {
			w.WriteHeader(http.StatusForbidden)
			w.Write(nil)
			return
		}
		userId := *session.Get("userId").(*uint64)
		if !kick.KickGuard(userId) {
			w.WriteHeader(http.StatusForbidden)
			w.Write(nil)
			return
		}
		user.WriteUpHandle(w, r, userId)
	})
	muxRouter.HandleFunc("/wp", func(w http.ResponseWriter, r *http.Request) {
		session := sessionManager.Start(w, r)
		isLogin := session.Get("isLogin")
		isAdmin := session.Get("isAdmin")
		if isLogin == nil || !*isLogin.(*bool) || isAdmin == nil || !*isAdmin.(*bool) {
			w.WriteHeader(http.StatusForbidden)
			w.Write(nil)
			return
		}
		params := r.URL.Query()
		userId := params.Get("userId")
		if userId == "" {
			w.WriteHeader(http.StatusBadRequest)
			w.Write(nil)
			return
		}
		admin.DownloadWPByUserId(w, r, userId)
	})
	muxRouter.HandleFunc("/allwp", func(w http.ResponseWriter, r *http.Request) {
		session := sessionManager.Start(w, r)
		isLogin := session.Get("isLogin")
		isAdmin := session.Get("isAdmin")
		if isLogin == nil || !*isLogin.(*bool) || isAdmin == nil || !*isAdmin.(*bool) {
			w.WriteHeader(http.StatusForbidden)
			w.Write(nil)
			return
		}
		admin.DownloadAllWP(w, r)
	})
	muxRouter.HandleFunc("/image", func(w http.ResponseWriter, r *http.Request) {
		session := sessionManager.Start(w, r)
		isLogin := session.Get("isLogin")
		isAdmin := session.Get("isAdmin")
		if isLogin == nil || !*isLogin.(*bool) || isAdmin == nil || !*isAdmin.(*bool) {
			w.WriteHeader(http.StatusForbidden)
			w.Write(nil)
			return
		}
		admin.UploadImage(w, r)
	})
	muxRouter.HandleFunc("/sse", sse.SSE.ServeHTTP)
	if HasFrontEnd {
		root, err := fs.Sub(staticFolder, "static")
		if err != nil {
			log.Panicln(err)
		}
		fileServer := http.FileServer(http.FS(root))
		indexFile, err := staticFolder.ReadFile("static/index.html")
		if err != nil {
			log.Panicln(err)
		}
		homeFile, err := staticFolder.ReadFile("static/home.html")
		if err != nil {
			log.Panicln(err)
		}
		if _, err := os.Stat("resources/home.html"); err == nil {
			homeFile, err = os.ReadFile("resources/home.html")
			if err != nil {
				log.Panicln(err)
			}
		}

		withGzipped := Gzip(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.URL.Path == "/home.html" {
				w.WriteHeader(http.StatusOK)
				w.Write(homeFile)
			} else if _, err := fs.Stat(root, r.URL.Path[1:]); err == nil {
				_, filename := filepath.Split(r.URL.Path)
				if filepath.Ext(r.URL.Path) == ".js" || filepath.Ext(r.URL.Path) == ".css" {
					if r.Header.Get("if-none-match") == filename {
						w.WriteHeader(http.StatusNotModified)
						return
					}
					w.Header().Set("etag", filename)
				}
				fileServer.ServeHTTP(w, r)
			} else {
				w.WriteHeader(http.StatusOK)
				w.Write(indexFile)
			}
		}))

		muxRouter.PathPrefix("/").Handler(withGzipped)
	}
	muxRouter.Use(CSRFMiddleware)
	http.Handle("/", muxRouter)
}

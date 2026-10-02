# Math Quiz

An arithmetic quiz (+, −, ×, ÷) written in ClojureScript with
[Reagent](https://reagent-project.github.io/), built with
[shadow-cljs](https://github.com/thheller/shadow-cljs).

Live at <https://mikeyreilly.github.io/math-quiz/>.

## Prerequisites

- Java 21 or later (needed by shadow-cljs)
- Node.js and npm

Then install the dependencies:

```sh
npm install
```

## Run locally

```sh
npx shadow-cljs watch frontend
```

Open <http://localhost:8080/>. Changes to the source reload in the browser
automatically.

## Tests

Tests live in `src/test`; any namespace ending in `-test` is picked up.

```sh
npx shadow-cljs watch test
```

Open <http://localhost:8021/> to see the results. They re-run on every save.
To run the app and the tests together, use `npx shadow-cljs watch frontend test`.

## Release build and deploy

The site is served by GitHub Pages from the `gh-pages` branch, which holds
only the built static site; `master` never contains generated JS.

```sh
npm run deploy
git push origin gh-pages
```

`npm run deploy` runs `scripts/deploy-gh-pages.sh`. The script runs
`npx shadow-cljs release frontend`, which writes an optimized `main.js` to
`out/site/js` (so it won't interfere with a running `watch`). It copies the
rest of `public/` alongside it and commits the result to `gh-pages`. It never
pushes.

To try the release build locally before pushing:

```sh
python3 -m http.server 8090 -d out/site
```

Then open <http://localhost:8090/>.

## Layout

- `src/main` – application source (`quaxt.arithmetic-challenge`)
- `src/test` – tests
- `public` – `index.html`, CSS, favicon; dev builds compile JS into `public/js`
- `scripts/deploy-gh-pages.sh` – builds the release and commits it to `gh-pages`

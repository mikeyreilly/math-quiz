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

## Release build

```sh
npx shadow-cljs release frontend
```

This writes an optimized `public/js/main.js`. Together with the rest of
`public/`, that is the whole static site. Stop any running `watch` first,
because it writes to the same file.

## Layout

- `src/main` – application source (`quaxt.arithmetic-challenge`)
- `src/test` – tests
- `public` – `index.html`, CSS, favicon; compiled JS goes to `public/js`

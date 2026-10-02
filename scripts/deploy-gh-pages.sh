#!/usr/bin/env bash
# Build an optimized release of the app and commit it to the gh-pages branch,
# which GitHub Pages serves at https://mikeyreilly.github.io/math-quiz/
#
# Usage:   scripts/deploy-gh-pages.sh      (or: npm run deploy)
# Then:    git push origin gh-pages
#
# master never contains generated JS; gh-pages contains only the static site.
set -euo pipefail

cd "$(dirname "$0")/.."

branch=gh-pages
site=out/site          # :release :output-dir in shadow-cljs.edn is out/site/js
worktree=out/gh-pages  # temporary checkout of the gh-pages branch

source_rev=$(git describe --always --dirty)
case "$source_rev" in
  *-dirty) echo "warning: deploying uncommitted changes" >&2 ;;
esac

# 1. Build the static site in out/site
rm -rf "$site"
npx shadow-cljs release frontend
rsync -a --exclude /js/ --exclude .DS_Store public/ "$site"/
rm -f "$site/js/manifest.edn"
touch "$site/.nojekyll"  # serve files as-is; skip GitHub's Jekyll processing

# 2. Check out gh-pages in a temporary worktree (creating the branch if needed)
cleanup() { git worktree remove --force "$worktree" 2>/dev/null || true; }
cleanup
git worktree prune
trap cleanup EXIT
if git show-ref --verify --quiet "refs/heads/$branch"; then
  git worktree add --quiet "$worktree" "$branch"
elif git show-ref --verify --quiet "refs/remotes/origin/$branch"; then
  git worktree add --quiet -b "$branch" "$worktree" "origin/$branch"
else
  git worktree add --quiet --orphan -b "$branch" "$worktree"
fi

# 3. Replace the branch contents with the site and commit
rsync -a --delete --exclude /.git "$site"/ "$worktree"/
git -C "$worktree" add -A
if git -C "$worktree" diff --cached --quiet; then
  echo "$branch is already up to date."
else
  git -C "$worktree" commit --quiet -m "Deploy $source_rev"
  echo "Committed $(git rev-parse --short "$branch") to $branch (built from $source_rev)."
  echo "Publish with: git push origin $branch"
fi

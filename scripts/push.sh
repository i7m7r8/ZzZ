#!/data/data/com.termux/files/usr/bin/bash
# Usage (in Termux):
#   bash scripts/push.sh https://github.com/YOUR_USER/ZzZ-VPN.git
# Or set REPO_URL env before running.

set -e

REPO_URL="${1:-$REPO_URL}"

if [ -z "$REPO_URL" ]; then
  echo "Usage: bash scripts/push.sh <github-repo-url>"
  echo "Example: bash scripts/push.sh https://github.com/i7m7r8/ZzZ-VPN.git"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

echo "==> Generating icons..."
python3 scripts/gen_icons.py

echo "==> Initialising git..."
if [ ! -d .git ]; then
  git init
  git branch -M main
fi

echo "==> Configuring remote..."
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"

echo "==> Staging all files..."
git add -A

echo "==> Committing..."
git commit -m "ZzZ VPN initial commit" --allow-empty

echo "==> Pushing to $REPO_URL ..."
git push -u origin main --force

echo ""
echo "✅ Done! Go to GitHub Actions and:"
echo "   1. Run 'Bootstrap Gradle Wrapper' workflow FIRST"
echo "   2. Then 'Build ZzZ VPN' will trigger automatically"
echo "   3. Download APK from Actions → Artifacts"

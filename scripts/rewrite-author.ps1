# Rewrite all commits on current branch to NealWizard <1054318578@qq.com>
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

git checkout develop

$author = 'NealWizard <1054318578@qq.com>'
$env:GIT_AUTHOR_NAME = 'NealWizard'
$env:GIT_AUTHOR_EMAIL = '1054318578@qq.com'
$env:GIT_COMMITTER_NAME = 'NealWizard'
$env:GIT_COMMITTER_EMAIL = '1054318578@qq.com'

# Use bash-compatible filter via git filter-branch on develop only
$env:GIT_FILTER_BRANCH_SQUELCH_WARNING = '1'
git filter-branch -f --env-filter "
export GIT_AUTHOR_NAME='NealWizard'
export GIT_AUTHOR_EMAIL='1054318578@qq.com'
export GIT_COMMITTER_NAME='NealWizard'
export GIT_COMMITTER_EMAIL='1054318578@qq.com'
" develop

Write-Host "`n=== develop after rewrite ==="
git log develop --oneline --format="%h %an <%ae> %s"

# Reset main to match develop if main is behind
git branch -f main develop
Write-Host "`n=== main synced to develop ==="
git log main --oneline --format="%h %an <%ae> %s"

Write-Host "`nNext: force push both branches"
Write-Host "  git push --force origin develop"
Write-Host "  git push --force origin main"

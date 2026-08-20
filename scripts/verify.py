#!/usr/bin/env python3
"""
Run before every push. Every check here exists because a red build taught it.

    python3 scripts/verify.py

Exit 0 and it is worth pushing. Exit 1 and CI would have told you the same thing five minutes
later, having spent five minutes.

WHY THIS EXISTS
---------------
Six of the last thirty builds were red, and not one of them was a design mistake. They were all the
same shape: a scripted edit that left a duplicate declaration, an orphaned annotation, an import
that was added twice or removed while still used, or a symbol used above the line that declares it.

Brace and paren balancing — the check this project already had — passes for every one of them,
because nothing about the *shape* of the file is wrong. These checks look at what the shape cannot
see.
"""

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "app/src/main/kotlin"
PREFS = SRC / "dev/patrickgold/florisboard/app/AppPrefs.kt"

problems: list[str] = []


def fail(where: str, msg: str) -> None:
    problems.append(f"{where}: {msg}")


def changed_files() -> list[Path]:
    """Only what this commit touches. A full-tree scan finds old debts and buries today's mistake."""
    out = subprocess.run(
        ["git", "-C", str(ROOT), "diff", "--name-only", "HEAD"],
        capture_output=True, text=True,
    ).stdout.split()
    staged = subprocess.run(
        ["git", "-C", str(ROOT), "diff", "--cached", "--name-only"],
        capture_output=True, text=True,
    ).stdout.split()
    files = {f for f in out + staged if f.endswith(".kt")}
    return [ROOT / f for f in sorted(files) if (ROOT / f).exists()]


def strip_code(text: str) -> str:
    """Comments and string bodies out, so counting is not fooled by prose."""
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    text = re.sub(r'"""(?:.|\n)*?"""', '""', text)
    text = re.sub(r'"(?:\\.|[^"\\])*"', '""', text)
    return text


def check_duplicate_imports(path: Path, text: str) -> None:
    """Red build: `MaKeys` imported twice — 'imported name is ambiguous'."""
    imports = [ln for ln in text.splitlines() if ln.startswith("import ")]
    seen = set()
    for imp in imports:
        if imp in seen:
            fail(path.name, f"duplicate import: {imp.split('.')[-1]}")
        seen.add(imp)


def check_import_order(path: Path, text: str) -> None:
    """Red build: imports hoisted above `package`, which does not compile."""
    lines = text.splitlines()
    pkg = next((i for i, ln in enumerate(lines) if ln.startswith("package ")), None)
    if pkg is None:
        return
    for i, ln in enumerate(lines):
        if ln.startswith("import ") and i < pkg:
            fail(path.name, "an import sits above the package declaration")
            return


def check_duplicate_declarations(path: Path, text: str) -> None:
    """
    Red build: the same preference declared twice, fifteen lines apart — 'conflicting declarations'.

    Scope is tracked by brace depth rather than by indentation. Two earlier versions used indent as a
    proxy and raised 454 and then 106 false alarms on code that compiles perfectly, because Kotlin is
    happy for the same name to exist in two different scopes — and a checker that cries wolf is a
    checker that gets ignored, which is worse than not having one.

    Only declarations sharing an enclosing scope are a conflict.
    """
    code = strip_code(text)
    scope: list[int] = []
    counter = 0
    seen: set[tuple[tuple[int, ...], str]] = set()
    decl = re.compile(r"^\s*(?:private |internal |public |@Volatile |const |lateinit )*va[lr] (\w+)\s*[:=]")

    for line in code.splitlines():
        m = decl.match(line)
        if m:
            key = (tuple(scope), m.group(1))
            if key in seen:
                fail(path.name, f"declared twice in the same scope: {m.group(1)}")
            seen.add(key)
        for ch in line:
            if ch == "{":
                counter += 1
                scope.append(counter)
            elif ch == "}" and scope:
                scope.pop()


def check_annotation_pairs(path: Path, text: str) -> None:
    """Red build: a replacement left the old @Composable above the new one — 'not repeatable'."""
    lines = text.splitlines()
    for i, ln in enumerate(lines):
        if ln.strip() != "@Composable":
            continue
        # what follows, skipping doc comments and other annotations
        j = i + 1
        while j < len(lines):
            nxt = lines[j].strip()
            if nxt.startswith(("/**", "*", "*/")) or nxt == "":
                j += 1
                continue
            break
        else:
            continue
        if lines[j].strip() == "@Composable":
            fail(path.name, f"two @Composable in a row, line {i + 1}")


def check_icon_imports(path: Path, text: str) -> None:
    """Red build: `Icons.Default.X` used with no import, in a file that compiled before."""
    imports = {ln.rsplit(".", 1)[-1].strip() for ln in text.splitlines() if ln.startswith("import ")}
    used = set(re.findall(r"Icons\.(?:Default|Filled|AutoMirrored\.Filled|Outlined)\.(\w+)", strip_code(text)))
    for icon in sorted(used - imports):
        fail(path.name, f"icon used but not imported: {icon}")


def check_delegation_imports(path: Path, text: str) -> None:
    """
    Red build in waiting: an unused-import sweep removed `getValue`.

    Property delegation — `val x by y` — needs getValue and never names it, so any tool that removes
    imports by searching for the symbol deletes it and the file stops compiling with an error
    pointing at the delegation rather than at the import.
    """
    code = strip_code(text)
    imports = {ln.strip() for ln in text.splitlines() if ln.startswith("import ")}
    has_val_by = re.search(r"\bval\s+\w+\s+by\s+", code)
    has_var_by = re.search(r"\bvar\s+\w+\s+by\s+", code)
    if has_val_by or has_var_by:
        if "import androidx.compose.runtime.getValue" not in imports and "compose" in code:
            fail(path.name, "uses `by` delegation but does not import getValue")
    if has_var_by:
        if "import androidx.compose.runtime.setValue" not in imports and "mutableStateOf" in code:
            fail(path.name, "uses `var ... by mutableStateOf` but does not import setValue")


def check_preferences_exist(path: Path, text: str) -> None:
    """Red build: a row named a preference that had been renamed out from under it."""
    if not PREFS.exists() or path == PREFS:
        return
    declared = set(re.findall(r"\bva[lr]\s+(\w+)\s*=\s*(?:boolean|string|int|long|float|enum|custom)\(", PREFS.read_text()))
    if not declared:
        return
    used = set(re.findall(r"prefs\.\w+\.(\w+)\b", strip_code(text)))
    known_other = {"get", "set", "collectAsState"}
    for name in sorted(used - declared - known_other):
        # only complain about things that look like our own preferences
        if name.startswith("ma"):
            fail(path.name, f"preference does not exist in AppPrefs: {name}")


def check_balance(path: Path, text: str) -> None:
    """The old check, kept: braces and parens against HEAD rather than against zero."""
    rel = path.relative_to(ROOT).as_posix()
    head = subprocess.run(
        ["git", "-C", str(ROOT), "show", f"HEAD:{rel}"], capture_output=True, text=True
    ).stdout
    now = strip_code(text)
    was = strip_code(head) if head else ""
    for ch_open, ch_close, label in (("{", "}", "braces"), ("(", ")", "parens")):
        d_now = now.count(ch_open) - now.count(ch_close)
        d_was = was.count(ch_open) - was.count(ch_close) if head else 0
        if d_now != d_was:
            fail(path.name, f"{label} unbalanced: {d_now} now, {d_was} at HEAD")


def check_when_coverage() -> None:
    """
    Red build: a new key added to the enum and one `when` never given its branch.

    Kotlin catches a non-exhaustive `when` that returns a value; it does NOT catch one used as a
    statement, which is exactly how a new key silently draws nothing.
    """
    for enum_file, users in (
        ("dictate/MaFeatureOrder.kt", ["dictate/ui/MaFeatureRow.kt", "app/settings/dictate/MaRowsScreen.kt"]),
        ("app/settings/MaSettingsOrder.kt", ["app/settings/MaSettingsOrderScreen.kt"]),
    ):
        ef = SRC / "dev/patrickgold/florisboard" / enum_file
        if not ef.exists():
            continue
        entries = set(re.findall(r"^\s{4}([A-Z][A-Z_0-9]*)\(\"", ef.read_text(), re.M))
        if not entries:
            continue
        for u in users:
            uf = SRC / "dev/patrickgold/florisboard" / u
            if not uf.exists():
                continue
            mentioned = set(re.findall(r"\b(?:MaFeatureKey|MaSettingsEntry)\.([A-Z][A-Z_0-9]*)", uf.read_text()))
            missing = entries - mentioned
            if missing:
                fail(uf.name, f"no branch for: {', '.join(sorted(missing))}")


def check_no_secrets() -> None:
    """A key in a commit is public the moment it is pushed, and history rewriting does not undo it."""
    diff = subprocess.run(
        ["git", "-C", str(ROOT), "diff", "HEAD"], capture_output=True, text=True
    ).stdout
    # Deny by default on long tokens after a known prefix, and both Gemini formats.
    pattern = re.compile(r"(sk_|gsk_|sk-|ghp_|github_pat_|AIza|AQ\.)[A-Za-z0-9_\-]{16,}")
    for m in pattern.finditer(diff):
        fail("STAGED DIFF", f"something key-shaped ({m.group(1)}…) — do not push")
        break


def main() -> int:
    files = changed_files()
    if not files:
        # Said precisely, because "nothing changed" when a file plainly did change reads as the tool
        # being broken, and a tool nobody believes is a tool nobody runs.
        print("no Kotlin files changed — nothing for this to check")
    for path in files:
        text = path.read_text()
        check_duplicate_imports(path, text)
        check_import_order(path, text)
        check_duplicate_declarations(path, text)
        check_annotation_pairs(path, text)
        check_icon_imports(path, text)
        check_delegation_imports(path, text)
        check_preferences_exist(path, text)
        check_balance(path, text)
    check_when_coverage()
    check_no_secrets()

    if problems:
        print(f"\n{len(problems)} problem(s):\n")
        for p in problems:
            print("  " + p)
        print("\nCI would have told you this in five minutes. Fix and re-run.")
        return 1
    print(f"checked {len(files)} file(s) — nothing to report")
    return 0


if __name__ == "__main__":
    sys.exit(main())

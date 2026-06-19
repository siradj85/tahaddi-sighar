# -*- coding: utf-8 -*-
"""يحوّل QuestionBank.kt إلى docs/questions.json (بذرة أولية للنظام عن بُعد)."""
import re, json, os

base = os.path.dirname(os.path.dirname(__file__))
src = os.path.join(base, "app/src/main/java/com/saidcharoun/tahaddisighar/QuestionBank.kt")
text = open(src, encoding="utf-8").read()

# q("e","t", listOf("o1","o2",...), c, age, d)
pat = re.compile(
    r'q\(\s*"((?:[^"\\]|\\.)*)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*,\s*listOf\(([^)]*)\)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)'
)
qstr = re.compile(r'"((?:[^"\\]|\\.)*)"')

out = []
for m in pat.finditer(text):
    e, t, opts_raw, c, age, d = m.groups()
    opts = qstr.findall(opts_raw)
    out.append({"e": e, "t": t, "o": opts, "c": int(c), "age": int(age), "d": int(d)})

data = {"version": 1, "questions": out}
os.makedirs(os.path.join(base, "docs"), exist_ok=True)
with open(os.path.join(base, "docs/questions.json"), "w", encoding="utf-8") as f:
    f.write(json.dumps(data, ensure_ascii=False, separators=(",", ":")))

from collections import Counter
ca = Counter(q["age"] for q in out)
print("total:", len(out), "by age:", dict(sorted(ca.items())))

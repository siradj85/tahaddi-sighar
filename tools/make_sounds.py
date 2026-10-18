import numpy as np, wave, random
SR = 44100

def t_(d): return np.arange(int(SR * d)) / SR

def bell(f, d=0.9, vol=1.0, bright=1.0):
    t = t_(d)
    out = np.zeros_like(t)
    for mult, w, dec in [(1, 1.0, 5.5), (2.0, 0.30 * bright, 8), (2.76, 0.28 * bright, 9), (5.4, 0.10 * bright, 14), (8.93, 0.04 * bright, 20)]:
        out += w * np.sin(2 * np.pi * f * mult * t) * np.exp(-dec * t)
    att = np.minimum(t / 0.002, 1)
    return out * att * vol

def marimba(f, d=0.5, vol=1.0):
    t = t_(d)
    out = np.sin(2 * np.pi * f * t) * np.exp(-7 * t) + 0.35 * np.sin(2 * np.pi * 4 * f * t) * np.exp(-22 * t) + 0.12 * np.sin(2 * np.pi * 9.2 * f * t) * np.exp(-40 * t)
    return out * np.minimum(t / 0.002, 1) * vol

def soft(f, d=0.5, vol=1.0, dec=4.0):
    t = t_(d)
    out = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * 2 * f * t) + 0.08 * np.sin(2 * np.pi * 3 * f * t)
    return out * np.exp(-dec * t) * np.minimum(t / 0.004, 1) * vol

def place(buf, snd, at):
    i = int(at * SR)
    n = min(len(snd), len(buf) - i)
    if n > 0:
        buf[i:i + n] += snd[:n]

def reverb(x, mix=0.22):
    out = x.copy()
    for dl, g in [(0.029, 0.35), (0.043, 0.28), (0.067, 0.22), (0.097, 0.16)]:
        k = int(dl * SR)
        d = np.zeros_like(x)
        d[k:] = x[:-k] * g
        out += d * mix * 2
    return out

def save(name, x, peak=0.8, fade=0.01):
    x = x / max(1e-9, np.abs(x).max()) * peak
    n = int(fade * SR)
    if n > 0: x[-n:] *= np.linspace(1, 0, n)
    x[:8] *= np.linspace(0, 1, 8)
    with wave.open(name, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())

D = 'C:/Users/MSI/AppData/Local/Temp/claude/snd/'

# click: soft wooden tick
t = t_(0.06)
click = (np.sin(2 * np.pi * 1500 * t) * np.exp(-90 * t) + 0.5 * np.sin(2 * np.pi * 700 * t) * np.exp(-60 * t))
save(D + 'snd_click.wav', click, peak=0.45)

# correct: bright two-note chime (major third up)
b = np.zeros(int(SR * 1.0))
place(b, bell(1046.5, 0.9), 0.0)
place(b, bell(1318.5, 0.9, 1.0), 0.11)
place(b, bell(1568.0, 0.8, 0.8), 0.22)
save(D + 'snd_correct.wav', reverb(b, 0.18), peak=0.75)

# wrong: gentle low 'bonk' with pitch drop (not harsh)
t = t_(0.5)
f = 230 * np.exp(-1.6 * t) + 120
ph = 2 * np.pi * np.cumsum(f) / SR
w = (np.sin(ph) + 0.3 * np.sin(2 * ph)) * np.exp(-7 * t) * np.minimum(t / 0.006, 1)
save(D + 'snd_wrong.wav', w, peak=0.6)

# life lost: two descending soft marimba notes
b = np.zeros(int(SR * 0.9))
place(b, marimba(392.0, 0.5), 0.0)
place(b, marimba(293.7, 0.6), 0.16)
place(b, soft(196.0, 0.6, 0.5, 5), 0.3)
save(D + 'snd_life_lost.wav', reverb(b, 0.12), peak=0.7)

# win: happy fanfare arpeggio + sparkle
b = np.zeros(int(SR * 2.4))
notes = [523.25, 659.25, 783.99, 1046.5, 1318.5]
for i, n in enumerate(notes):
    place(b, bell(n, 1.2, 0.9), i * 0.13)
chord = [523.25, 659.25, 783.99, 1046.5]
for n in chord:
    place(b, soft(n, 1.6, 0.35, 2.2), 0.7)
for i, n in enumerate([2093.0, 2637.0, 3136.0, 2637.0, 3136.0, 4186.0]):
    place(b, bell(n, 0.5, 0.18), 0.72 + i * 0.06)
save(D + 'snd_win.wav', reverb(b, 0.25), peak=0.8, fade=0.08)

# music: calm playful loop, 12 bars at 96 bpm in C major (I vi IV V x3)
bpm = 96; beat = 60 / bpm; bar = beat * 4
total = bar * 12
buf = np.zeros(int(total * SR) + int(3 * SR))
N = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}
def hz(note, octv): return 440.0 * 2 ** ((N[note] - 9 + 12 * (octv - 4)) / 12)
prog = [('C', ['C', 'E', 'G']), ('A', ['A', 'C', 'E']), ('F', ['F', 'A', 'C']), ('G', ['G', 'B', 'D'])]
random.seed(7)
penta = [('C', 5), ('D', 5), ('E', 5), ('G', 5), ('A', 5), ('C', 6)]
for b_i in range(12):
    root, tri = prog[b_i % 4]
    start = b_i * bar
    # bass on beats 1 and 3
    place(buf, soft(hz(root, 3), 1.4, 0.55, 2.6), start)
    place(buf, soft(hz(root, 3) * (1.5 if root != 'F' else 1.5), 1.0, 0.3, 3.2), start + 2 * beat)
    # pad
    for n in tri:
        place(buf, soft(hz(n, 4), bar * 1.1, 0.13, 1.1), start)
    # marimba arpeggio (eighths)
    arp = [tri[0], tri[1], tri[2], tri[1]]
    for e in range(8):
        n = arp[e % 4]
        place(buf, marimba(hz(n, 5), 0.5, 0.33), start + e * beat / 2)
    # melody: sparse bell notes
    pos = 0
    while pos < 4:
        if random.random() < 0.7:
            nm, oc = random.choice(penta)
            place(buf, bell(hz(nm, oc), 0.9, 0.26, 0.6), start + pos * beat)
        pos += random.choice([1, 1, 2])
buf = reverb(buf, 0.2)
L = int(total * SR)
loop = buf[:L].copy()
tail = buf[L:L + int(3 * SR)]
loop[:len(tail)] += tail  # wrap the ringing tail onto the start for a seamless loop
save(D + 'snd_music.wav', loop, peak=0.7, fade=0.0)
print('ok')

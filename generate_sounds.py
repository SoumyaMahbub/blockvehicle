import math
import struct
import wave
import subprocess
import os

SAMPLE_RATE = 44100

def clamp(val, min_val=-1.0, max_val=1.0):
    return max(min_val, min(max_val, val))

def write_wav(filename, samples):
    with wave.open(filename, 'w') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        int_samples = [int(clamp(s) * 32767.0) for s in samples]
        raw = struct.pack('<' + 'h' * len(int_samples), *int_samples)
        w.writeframes(raw)

def convert_to_ogg(wav_path, ogg_path):
    subprocess.run([
        'ffmpeg', '-y', '-i', wav_path,
        '-c:a', 'libvorbis', '-q:a', '5',
        ogg_path
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if os.path.exists(wav_path):
        os.remove(wav_path)

# Simple Pseudo-Random Noise generator
class NoiseGen:
    def __init__(self, seed=12345):
        self.state = seed
    def next(self):
        self.state = (self.state * 1103515245 + 12345) & 0x7fffffff
        return (self.state / 0x7fffffff) * 2.0 - 1.0

# ─── 1. Engine Idle (Seamless Loop) ──────────────────────────────────────────
def gen_engine_idle(duration=2.0):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(42)
    
    # Exact integer cycles for seamless loop
    # 52.5 Hz * 2.0s = 105 cycles exactly!
    f0 = 105.0 / duration 
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        phase = 2.0 * math.pi * f0 * t
        
        # 4-cylinder firing wobble
        wobble = 1.0 + 0.12 * math.sin(2.0 * math.pi * (f0 / 4.0) * t)
        
        # Harmonics
        val = (
            0.55 * math.sin(phase) +
            0.35 * math.sin(2 * phase + 0.5) +
            0.25 * math.sin(3 * phase + 1.2) +
            0.18 * math.sin(4 * phase + 0.2) +
            0.12 * math.sin(6 * phase + 0.8) +
            0.08 * math.sin(8 * phase + 1.5)
        ) * wobble
        
        # Exhaust puff / air rush noise (low-pass feel)
        n = noise.next() * 0.08 * (0.6 + 0.4 * math.sin(phase * 2.0))
        val += n
        
        # Slight distortion for combustion grit
        val = math.tanh(val * 1.3) * 0.75
        samples.append(val)
    return samples

# ─── 2. Engine Rev / Running (Seamless Loop) ─────────────────────────────────
def gen_engine_rev(duration=2.0):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(99)
    
    # Exact integer cycles: 140 Hz * 2.0s = 280 cycles exactly!
    f0 = 280.0 / duration
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        phase = 2.0 * math.pi * f0 * t
        
        # Rich asymmetric combustion wave
        val = (
            0.45 * math.sin(phase) +
            0.30 * math.sin(2 * phase + 0.3) +
            0.25 * math.sin(3 * phase + 0.9) +
            0.20 * math.sin(4 * phase + 1.4) +
            0.15 * math.sin(5 * phase + 0.2) +
            0.12 * math.sin(6 * phase + 0.7) +
            0.08 * math.sin(8 * phase + 1.1) +
            0.05 * math.sin(10 * phase + 1.8)
        )
        
        # Turbo / intake spool whistle (~2200 Hz)
        turbo_phase = 2.0 * math.pi * 2205.0 * t
        turbo = 0.04 * math.sin(turbo_phase)
        
        # Exhaust rush
        n = noise.next() * 0.09 * (0.7 + 0.3 * math.sin(phase * 2.0))
        
        val = math.tanh((val + turbo + n) * 1.4) * 0.8
        samples.append(val)
    return samples

# ─── 3. Engine Start ─────────────────────────────────────────────────────────
def gen_engine_start():
    duration = 1.8
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(101)
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        val = 0.0
        
        if t < 0.7:
            # Cranking phase: 3 starter motor chugs
            crank_cycle = (t % 0.23) / 0.23
            crank_pulse = math.sin(math.pi * crank_cycle) ** 4
            starter_whine = 0.15 * math.sin(2.0 * math.pi * 520.0 * t)
            compression_thud = 0.45 * math.sin(2.0 * math.pi * 45.0 * t) * crank_pulse
            gear_noise = noise.next() * 0.08 * crank_pulse
            val = starter_whine + compression_thud + gear_noise
        elif t < 1.3:
            # Ignition burst / engine catching
            prog = (t - 0.7) / 0.6
            f = 60.0 + (180.0 - 60.0) * math.sin(math.pi * prog)
            phase = 2.0 * math.pi * f * t
            burst_env = math.exp(-2.5 * prog) * 0.9 + 0.4
            combustion = (math.sin(phase) + 0.5 * math.sin(2 * phase) + 0.3 * math.sin(3 * phase)) * burst_env
            exhaust_blast = noise.next() * 0.15 * math.exp(-4.0 * prog)
            val = combustion + exhaust_blast
        else:
            # Settling into idle
            prog = (t - 1.3) / 0.5
            f = 80.0 - 28.0 * prog
            phase = 2.0 * math.pi * f * t
            val = (0.5 * math.sin(phase) + 0.3 * math.sin(2 * phase)) * 0.5
        
        samples.append(math.tanh(val * 1.3) * 0.8)
    return samples

# ─── 4. Engine Stop ──────────────────────────────────────────────────────────
def gen_engine_stop():
    duration = 1.2
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(202)
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        prog = t / duration
        f = max(18.0, 75.0 * (1.0 - prog)**1.8)
        phase = 2.0 * math.pi * f * t
        env = math.exp(-2.2 * prog)
        val = (math.sin(phase) + 0.4 * math.sin(2 * phase) + noise.next() * 0.06) * env * 0.7
        if 0.75 <= t <= 0.82:
            # Mechanical click
            val += 0.35 * math.sin(2.0 * math.pi * 1400.0 * t) * math.exp(-30.0 * (t - 0.75))
        samples.append(clamp(val * 0.8))
    return samples

# ─── 5. Car Horn ─────────────────────────────────────────────────────────────
def gen_car_horn(duration=0.75):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    
    f1 = 435.0 # Low tone (F)
    f2 = 545.0 # High tone (A)
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        
        # Envelope: 15ms attack, sustain, 60ms release
        if t < 0.015:
            env = t / 0.015
        elif t > duration - 0.06:
            env = (duration - t) / 0.06
        else:
            env = 1.0
            
        vibrato = 1.0 + 0.006 * math.sin(2.0 * math.pi * 6.0 * t)
        
        phase1 = 2.0 * math.pi * f1 * vibrato * t
        phase2 = 2.0 * math.pi * f2 * vibrato * t
        
        # Tone 1 harmonics
        h1 = 0.50 * math.sin(phase1) + 0.28 * math.sin(2 * phase1) + 0.18 * math.sin(3 * phase1) + 0.10 * math.sin(4 * phase1)
        # Tone 2 harmonics
        h2 = 0.45 * math.sin(phase2) + 0.25 * math.sin(2 * phase2) + 0.15 * math.sin(3 * phase2) + 0.08 * math.sin(4 * phase2)
        
        val = (h1 + h2) * env * 0.65
        samples.append(math.tanh(val * 1.2) * 0.85)
    return samples

# ─── 6. Tire Skid ────────────────────────────────────────────────────────────
def gen_tire_skid(duration=0.9):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(777)
    
    # Filter state
    b0, b1, b2 = 0.0, 0.0, 0.0
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        env = math.sin(math.pi * (t / duration)) ** 0.5
        
        # Modulated screech frequencies
        screech1 = math.sin(2.0 * math.pi * (1650.0 + 200.0 * math.sin(2.0 * math.pi * 85.0 * t)) * t)
        screech2 = math.sin(2.0 * math.pi * (2450.0 + 350.0 * math.sin(2.0 * math.pi * 110.0 * t)) * t)
        
        raw_n = noise.next()
        # Simple IIR bandpass for rubber friction scrub
        b0 = 0.7 * b0 + 0.3 * raw_n
        b1 = 0.6 * b1 + 0.4 * b0
        
        val = (0.35 * screech1 + 0.25 * screech2 + 0.40 * b1) * env * 0.75
        samples.append(clamp(val))
    return samples

# ─── 7. Car Crash / Impact ───────────────────────────────────────────────────
def gen_car_impact():
    duration = 1.3
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(888)
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        
        # 1. Low sub-bass hit transient
        sub = 0.85 * math.sin(2.0 * math.pi * 55.0 * (1.0 - t * 0.4) * t) * math.exp(-8.0 * t)
        
        # 2. Metal crunch and clang resonance (320 Hz + 680 Hz + 1150 Hz)
        metal = (
            0.5 * math.sin(2.0 * math.pi * 320.0 * t) +
            0.35 * math.sin(2.0 * math.pi * 680.0 * t) +
            0.25 * math.sin(2.0 * math.pi * 1150.0 * t)
        ) * math.exp(-6.0 * t)
        
        # 3. Violent friction / crumple burst
        crumple = noise.next() * math.exp(-7.0 * t) * 0.75
        
        # 4. Debris shudder
        debris = noise.next() * 0.15 * math.exp(-3.0 * t)
        
        val = (sub + metal + crumple + debris)
        samples.append(math.tanh(val * 1.5) * 0.9)
    return samples

# ─── 8. Car Door Close ───────────────────────────────────────────────────────
def gen_car_door_close():
    duration = 0.45
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(333)
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        val = 0.0
        
        # Latch click (0 - 40ms)
        if t < 0.04:
            click = math.sin(2.0 * math.pi * 2100.0 * t) * math.exp(-80.0 * t) * 0.4
            val += click
            
        # Heavy door seal thud (30ms - 250ms)
        if t >= 0.02:
            t_thud = t - 0.02
            thud = math.sin(2.0 * math.pi * 75.0 * t_thud) * math.exp(-18.0 * t_thud) * 0.85
            resonance = math.sin(2.0 * math.pi * 160.0 * t_thud) * math.exp(-22.0 * t_thud) * 0.4
            noise_puff = noise.next() * 0.25 * math.exp(-25.0 * t_thud)
            val += thud + resonance + noise_puff
            
        samples.append(clamp(val * 0.85))
    return samples

# ─── 9. Car Door Open ────────────────────────────────────────────────────────
def gen_car_door_open():
    duration = 0.35
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    noise = NoiseGen(444)
    
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        val = 0.0
        
        # Handle pull click
        if t < 0.05:
            val += math.sin(2.0 * math.pi * 1800.0 * t) * math.exp(-90.0 * t) * 0.5
        # Latch pop
        if t >= 0.04:
            t_pop = t - 0.04
            pop = math.sin(2.0 * math.pi * 320.0 * t_pop) * math.exp(-35.0 * t_pop) * 0.6
            seal = noise.next() * 0.18 * math.exp(-40.0 * t_pop)
            val += pop + seal
            
        samples.append(clamp(val * 0.75))
    return samples

def main():
    target_dir = os.path.join('src', 'main', 'resources', 'assets', 'blockvehicle', 'sounds')
    os.makedirs(target_dir, exist_ok=True)
    
    sounds = {
        'engine_idle': gen_engine_idle(),
        'engine_rev': gen_engine_rev(),
        'engine_start': gen_engine_start(),
        'engine_stop': gen_engine_stop(),
        'car_horn': gen_car_horn(),
        'tire_skid': gen_tire_skid(),
        'car_impact': gen_car_impact(),
        'car_door_close': gen_car_door_close(),
        'car_door_open': gen_car_door_open()
    }
    
    for name, sample_data in sounds.items():
        wav_file = os.path.join(target_dir, f"{name}.wav")
        ogg_file = os.path.join(target_dir, f"{name}.ogg")
        write_wav(wav_file, sample_data)
        convert_to_ogg(wav_file, ogg_file)
        print(f"Generated {ogg_file} ({len(sample_data)} samples)")

if __name__ == '__main__':
    main()

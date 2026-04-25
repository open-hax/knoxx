import numpy as np
from scipy.io import wavfile
import random

def generate_latent_walk():
    sample_rate = 44100
    duration = 30  # seconds
    t = np.linspace(0, duration, int(sample_rate * duration), endpoint=False)
    
    # 1. THE BED: Low-frequency drone
    # Sine wave at 55Hz (A1) with a very slow amplitude modulation
    drone_sine = np.sin(2 * np.pi * 55 * t)
    # Low-pass filtered-style noise: slow random walk for amplitude
    noise = np.random.normal(0, 0.1, len(t))
    # Simple low-pass approximation: cumulative sum (integration)
    drone_noise = np.cumsum(noise) * 0.01 
    # Normalize and blend
    bed = (drone_sine * 0.3) + (drone_noise * 0.2)
    
    # 2. THE QUERIES: Semantic Pings
    pings = np.zeros(len(t))
    num_pings = 25
    for _ in range(num_pings):
        start_idx = random.randint(0, len(t) - 2000)
        # High pitch "ping" (between 1kHz and 4kHz)
        freq = random.uniform(1000, 4000)
        ping_duration = 0.1 # seconds
        ping_t = np.linspace(0, ping_duration, int(sample_rate * ping_duration), endpoint=False)
        # Exponential decay
        t_env = np.linspace(0, 5, len(ping_t))
        envelope = np.exp(-t_env)
        ping_wave = np.sin(2 * np.pi * freq * ping_t) * envelope
        
        end_idx = start_idx + len(ping_wave)
        if end_idx < len(pings):
            pings[start_idx:end_idx] += ping_wave * 0.2

    # 3. THE FRAGMENT: The emerging melody
    # C Phrygian: C(0), Db(1), Eb(3), F(5), G(7), Ab(8), Bb(10)
    melody_notes = [0, 1, 3, 0, 8, 7, 0] # Root-ish movements
    frequencies = {0: 130.81, 1: 138.59, 3: 155.56, 7: 196.00, 8: 207.65}
    
    fragment = np.zeros(len(t))
    current_time = 10  # Start emerging after 10s
    
    for note_val in melody_notes:
        if current_time >= duration: break
        
        freq = frequencies.get(note_val, 130.81)
        note_len = 1.5
        note_t = np.linspace(0, note_len, int(sample_rate * note_len), endpoint=False)
        
        # Emerge: Start muffled (low amplitude) and fade in
        # We simulate a "filter opening" by modulating the gain over the song duration
        gain = (current_time - 10) / (duration - 10) * 0.3
        wave = np.sin(2 * np.pi * freq * note_t) * 0.5 * gain
        
        start_idx = int(current_time * sample_rate)
        end_idx = start_idx + len(wave)
        if end_idx < len(fragment):
            fragment[start_idx:end_idx] += wave
            
        current_time += note_len + 0.5 # Add some space between notes

    # Final Mix
    mixed = bed + pings + fragment
    # Normalize to prevent clipping
    mixed = mixed / np.max(np.abs(mixed))
    
    # Convert to 16-bit PCM
    audio_data = (mixed * 32767).astype(np.int16)
    wavfile.write('latent_space_walk_seed.wav', sample_rate, audio_data)
    print("Generated: latent_space_walk_seed.wav")

if __name__ == "__main__":
    generate_latent_walk()

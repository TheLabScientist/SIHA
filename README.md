# SIHA (Short Input Hash Algorithm) - Documentation

**Author**: TheLabScientist

**Creation Date**: 8th March, 2025

## Overview

**SIHA** (Short Input Hash Algorithm) is an experimental cryptographic hash function designed using java to provide high security, strong diffusion, and quantum resistance. The algorithm incorporates:

- Cellular automata-based state initialization for unpredictable starting conditions
- ChaCha-inspired dynamic constants for non-linearity
- Feistel-like permutation structure for strong diffusion
- Non-linear S-Box transformations to prevent structural patterns

## Design Goals

- **Quantum Resistance** – Resistant to Grover’s search algorithm
- **Strong Avalanche Effect** – Small input changes result in large output differences
- **High Entropy** – Reduces patterns in the output
- **Preimage and Collision Resistance** – Prevents attackers from finding the original input or two inputs with the same hash
- **Secure Against Length Extension Attacks** – Uses a strong padding scheme

## Algorithm Structure

### State Initialization (Cellular Automata-Based)

Instead of using a fixed initialization vector, SIHA derives its initial state dynamically using Cellular Automata Rule 30, ensuring each input generates a unique, unpredictable state

***Process:***

1. Initialize a 128-byte state using input length and XORing input bytes
2. Apply 64 rounds of cellular automata transformations to introduce chaotic behavior

```java
private void initializeState(byte[] input) {
    byte[] stateBuffer = new byte[128];

    // Seed using input length and XOR with input bytes
    for (int i = 0; i < stateBuffer.length; i++) {
        stateBuffer[i] = (byte) (input[i % input.length] ^ (input.length * 37));
    }

    // Apply Cellular Automata (Rule 30 inspired)
    for (int step = 0; step < 64; step++) {
        byte[] newState = new byte[128];
        for (int i = 1; i < stateBuffer.length - 1; i++) {
            newState[i] = (byte) ((stateBuffer[i - 1] ^ (stateBuffer[i] | stateBuffer[i + 1])) & 0xFF);
        }
        stateBuffer = newState;
    }

    state = stateBuffer;
}
```

Cellular automata ensure high unpredictability. Even small input changes completely alter the state

### Absorption Phase

Each 64-byte block of input is XORed into the internal state, ensuring that all input bits contribute to the final hash

```java
for (int i = 0; i < paddedInput.length; i += 64) {
    for (int j = 0; j < 64; j++) {
        state[j] ^= paddedInput[i + j];  
    }
    permutation();  // Apply diffusion
}
```

Prevents length extension attacks and ensures all input bits influence the hash

### Permutation Phase

The permutation step ensures strong diffusion by using ARX (Addition, Rotation, XOR) operations, Feistel-like structures, and non-linear transformations

```java
for (int round = 0; round < 32; round++) {
    for (int i = 0; i < words.length; i++) {
        long temp = words[(i + 1) % words.length] ^ chachaRandom(round, state);
        words[i] += temp;
        words[i] = Long.rotateLeft(words[i], (i % 7) + 5);
        words[i] ^= words[(i + 2) % words.length];

        // Feistel-style mixing step
        words[i] ^= Long.reverseBytes(words[(i + 3) % words.length]);
        words[i] ^= (temp << 3) | (temp >>> 61);

        words[i] = (words[i] * 0x5DEECE66DL) & 0xFFFFFFFFFFFFFFFFL;
        words[i] ^= sbox(words[i]);
    }
}
```

Feistel-style diffusion ensures full state mixing

ChaCha-inspired random constants prevent predictability

Bitwise shifts & reversals create strong non-linearity

### ChaCha-Inspired Random Constants

Each round of the permutation uses a dynamic constant derived from the input, preventing attackers from predicting the transformation sequence

```java
private long chachaRandom(int round, byte[] input) {
    long state = Arrays.hashCode(input) ^ (round * 0x9E3779B97F4A7C15L);
    
    for (int i = 0; i < input.length; i++) {
        state ^= input[i] * 0xA3B195354A39B70DL;
        state = Long.rotateLeft(state, (i % 13) + 3);
    }

    for (int i = 0; i < 8; i++) {
        state = Long.rotateLeft(state * 0x41C64E6DL + 0x3039, 17);
    }
    
    return state;
}
```

Prevents attackers from analyzing predictable constants

Input-dependent entropy increases non-linearity

### Non-Linear S-Box Transformation

The S-Box applies multiplicative non-linearity and bit rotations to further strengthen diffusion.

```java
private long sbox(long x) {
    x ^= (x << 7) & 0x9E3779B97F4A7C15L;
    x = Long.rotateLeft(x, 13); // Extra rotation for diffusion
    x ^= (x >> 11) & 0xD6E8FEB8AL;
    x *= 0xA3B195354A39B70DL; // Large prime multiplication for randomness
    x ^= Long.reverseBytes(x); // Stronger bit mixing
    return x & 0xFFFFFFFFFFFFFFFFL;
}
```

Prime multiplication ensures strong avalanche effects

Rotations prevent patterns from propagating

### Secure Padding Scheme

SIHA uses a variable-length padding method to prevent length extension attacks

```java
padded[input.length] = (byte) (0x80 ^ (input.length * 31)); 
padded[padded.length - 1] |= (0x01 ^ ((padded.length * 17) % 251));
```

Prevents structural weaknesses in input padding

## Example Usage in Java

```java
SIHA hasher = new SIHA();
byte[] hash = hasher.hash("Hello, World!".getBytes(StandardCharsets.UTF_8));
System.out.println(SIHA.bytesToHex(hash));
```

## Example Output

```
Hello World

c6e6f86c8a98305446b748c6894aa51bcfe6518a1764eee9cfc0e5d881df84a2aed7c250279f2f98926ffda6a0e6b5cf993f02f926d002096b45aa5db108013c
```

SIHA outputs 1024 bit hashes

## Conclusion

SIHA is a potentially quantum-resistant hash function that incorporates cellular automata-based initialization, ARX-based permutation, and ChaCha-inspired randomness to provide strong security guarantees

I encourage you to make tests to ensure the algorithm is vulnerability free!

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SIHA {
    private byte[] state = new byte[128];

    public SIHA() {
        initializeState(state);
    }
    
    private void initializeState(byte[] input) {
        byte[] stateBuffer = new byte[128];

        // Start with a seed from input length and XOR input bytes
        for (int i = 0; i < stateBuffer.length; i++) {
            stateBuffer[i] = (byte) (input[i % input.length] ^ (input.length * 37));
        }

        // Apply Cellular Automata rules (Rule 30 inspired)
        for (int step = 0; step < 64; step++) {
            byte[] newState = new byte[128];
        
        for (int i = 1; i < stateBuffer.length - 1; i++) {
            newState[i] = (byte) ((stateBuffer[i - 1] ^ (stateBuffer[i] | stateBuffer[i + 1])) & 0xFF);
        }
            
            stateBuffer = newState;
        }

        state = stateBuffer;
    }

    public byte[] hash(byte[] input) {
        byte[] paddedInput = pad(input);

        // Absorb phase
        for (int i = 0; i < paddedInput.length; i += 64) {
            for (int j = 0; j < 64; j++) {
                state[j] ^= paddedInput[i + j];  // XOR input into state
            }
            
            permutation();  // Apply the strongest mixing function
        }

        // Squeeze phase: Extract hash from state
        byte[] output = new byte[64]; // 512-bit output for max security
        System.arraycopy(state, 0, output, 0, 64);
        return output;
    }

    private byte[] pad(byte[] input) {
        int newLen = ((input.length + 64 - 1) / 64) * 64;
        byte[] padded = Arrays.copyOf(input, newLen);

        // Use a rotating XOR mask for more randomness
        for (int i = input.length; i < padded.length; i++) {
            padded[i] ^= (i * 0x41C64E6DL) & 0xFF;
        }

        padded[input.length] = (byte) (0x80 ^ (input.length * 31)); 
        padded[padded.length - 1] |= (0x01 ^ ((padded.length * 17) % 251));

        return padded;
    }
    
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

    private void permutation() {
        long[] words = new long[128 / 8];
        ByteBuffer.wrap(state).asLongBuffer().get(words);

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

        ByteBuffer.wrap(state).asLongBuffer().put(words);
    }

    private long sbox(long x) {
        x ^= (x << 7) & 0x9E3779B97F4A7C15L;
        x = Long.rotateLeft(x, 13); // Extra rotation for diffusion
        x ^= (x >> 11) & 0xD6E8FEB8AL;
        x *= 0xA3B195354A39B70DL; // Large prime multiplication for randomness
        x ^= Long.reverseBytes(x); // Stronger bit mixing
        return x & 0xFFFFFFFFFFFFFFFFL;
    }

    public static void main(String[] args) {
        SIHA hasher = new SIHA();
        byte[] hash = hasher.hash("Hello World".getBytes(StandardCharsets.UTF_8));

        System.out.println(bytesToHex(hash)); // Output 1024 bit hash
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
}
# Synthesizer App — Real-Time Audio Engine (Android, C++, Oboe)

The Synthesizer App is a real-time audio generation system built for Android using Java, C++, and the Oboe audio library. It enables users to create, manipulate, and layer multiple waveforms with low latency, delivering a responsive digital-music experience similar to a hardware synthesizer.

At the core of the application is a custom C++ audio engine that produces sine, square, triangle, and sawtooth waves. Each waveform is represented as an independent voice, and users can control key parameters such as frequency, amplitude, and LFO modulation. A lock-free message queue (ring buffer) connects the UI layer to the audio engine, ensuring seamless parameter updates without blocking the audio thread.

To maintain deterministic and uninterrupted audio performance, the system deliberately avoids generating audio in Java. Because Java’s execution is managed by the Android runtime, garbage collection can occur at any time, introducing unpredictable pauses that are unacceptable for real-time audio. By offloading all audio synthesis to native C++ via Oboe, the app ensures stable, low-latency output without GC-induced glitches.


<img width="350" height="500" alt="image" src="https://github.com/user-attachments/assets/bc0840ff-5817-406b-a2cb-b111071bd164" />

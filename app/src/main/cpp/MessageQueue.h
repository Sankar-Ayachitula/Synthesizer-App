#pragma once
#include <atomic>

enum MessageType {
    MSG_ADD_WAVE = 0,
    MSG_REMOVE_WAVE,
    MSG_SET_WAVE_FREQ,
    MSG_SET_WAVE_AMP,
    MSG_SET_WAVE_LFO_FREQ,   // NEW
    MSG_SET_WAVE_LFO_DEPTH,   // NEW
    MSG_SET_WAVE_TYPE,
};

struct Message {
    MessageType type;
    int   id;    // waveId or waveType (for ADD)
    float v1;    // value (freq/amp/depth)
    float v2;    // extra if needed
};

class MessageQueue {
public:
    static const int kSize = 256;

    Message buffer[kSize];
    std::atomic<int> head{0};
    std::atomic<int> tail{0};

    bool push(const Message &m) {
        int h = head.load(std::memory_order_relaxed);
        int next = (h + 1) % kSize;
        if (next == tail.load(std::memory_order_acquire)) {
            // queue full; drop msg
            return false;
        }
        buffer[h] = m;
        head.store(next, std::memory_order_release);
        return true;
    }

    bool pop(Message &m) {
        int t = tail.load(std::memory_order_relaxed);
        if (t == head.load(std::memory_order_acquire)) {
            return false; // empty
        }
        m = buffer[t];
        tail.store((t + 1) % kSize, std::memory_order_release);
        return true;
    }
};

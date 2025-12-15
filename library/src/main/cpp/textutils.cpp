#include <jni.h>
#include <string>
#include <cstring>

static int hx(char x) {
    if (x >= '0' && x <= '9') return x - '0';
    if (x >= 'a' && x <= 'f') return x - 'a' + 10;
    if (x >= 'A' && x <= 'F') return x - 'A' + 10;
    return -1;
}

extern "C" jboolean
Java_com_cyber_ads_utils_TextUtils_contains(
        JNIEnv *e,
        jobject o,
        jstring s
) {
    if (!s) return JNI_FALSE;

    const char *c = e->GetStringUTFChars(s, nullptr);
    if (!c) return JNI_FALSE;

    std::string r(c);
    e->ReleaseStringUTFChars(s, c);

    std::string t;
    t.reserve(r.size());
    for (char ch: r) {
        if (ch != ' ') t.push_back(ch);
    }
    if (t.empty()) return JNI_FALSE;

    auto p = t.find(':');
    if (p != std::string::npos) t = t.substr(0, p);
    if (t.empty()) return JNI_FALSE;

    static const unsigned char q[] = {
            0xB9, 0x9D, 0xF0, 0x6E, 0x3B, 0x8A, 0xD4, 0xE1,
            0x37, 0x05, 0xA8, 0x99, 0xF2, 0xDD, 0x01, 0x67
    }; // real_key ^ 0xAA

    unsigned char k[sizeof(q)];
    for (size_t i = 0; i < sizeof(q); ++i) {
        k[i] = static_cast<unsigned char>(q[i] ^ 0xAA);
    }

    static const char* h[] = {
            "52592faaf249112ff8df70463d15ca",        // "Anunciodeprueba"
            "525934abff431b3ff8dc76",                // "Annoncetest"
            "feb2d6281b8493c12545b6a2b2c40b",        // "테스트광고"
            "475229b0d044",                          // "TestAd"
            "525934b1ff431724f9c676562b03c4",        // "Annuncioditesto"
            "475229b0f04e042ef4c867",                // "Testanzeige"
            "4752298dfa4c1f25",                      // "TesIklan"
            "4242bb7e324e19285e0e6d47309610607d5032ad709bf926", // "Quảngcáothửnghiệm"
            "5259997eff431724f9ca76562b03ce",        // "Anúnciodeteste"
            "f391f02437909eec1d4fa4a6b8d0262db580ba622fc0d8e57d0880d3fec54b6b86d7fc687186c1ab3b33e294d5970d53f391e424378a9eed35", // "পরীক্ষামূলকবিজ্ঞাপন"
            "f393c624359e9eef1c4fa6a9b8d31e2db788ba600dc0dbc67d0b9cd3fcc94b69b9d7fe6c", // "जाँचविज्ञापन"
            "cb92827d48a4a6ec4429da9980db737ccabd826c48aa", // "إعلانتجريبي"
            "c3958a7140a1afc94d11d28188c97b78c3898a7540aaafc44d1dd28888c27b70c38f8a71"  // "Тестовоеобъявление"
    };

    const size_t HN = sizeof(h) / sizeof(h[0]);

    for (size_t idx = 0; idx < HN; ++idx) {
        const char *s0 = h[idx];
        size_t L = std::strlen(s0);
        if ((L & 1u) != 0u) continue;

        size_t n = L / 2;
        std::string d;
        d.resize(n);

        bool re = false;
        for (size_t j = 0, z = 0; j < L; j += 2, ++z) {
            int hi = hx(s0[j]);
            int lo = hx(s0[j + 1]);
            if (hi < 0 || lo < 0) {
                re = true;
                break;
            }
            unsigned char b = static_cast<unsigned char>((hi << 4) | lo);
            unsigned char v = static_cast<unsigned char>(b ^ k[z % sizeof(k)]);
            d[z] = static_cast<char>(v);
        }
        if (re) continue;

        if (!d.empty() && d.find(t) != std::string::npos) {
            return JNI_TRUE;
        }
    }

    return JNI_FALSE;
}

extern "C" jboolean
Java_com_cyber_ads_utils_TextUtils_isNotNull(
        JNIEnv *e,
        jobject o,
        jstring someStr
) {
    if (!someStr) return JNI_FALSE;

    const char *c = e->GetStringUTFChars(someStr, nullptr);
    if (!c) return JNI_FALSE;

    std::string r(c);
    e->ReleaseStringUTFChars(someStr, c);

    std::transform(r.begin(), r.end(), r.begin(),
                   [](unsigned char ch) { return std::tolower(ch); });

    if (r.empty()) return JNI_FALSE;

    // same XOR key as in contains()
    static const unsigned char q[] = {
            0xB9, 0x9D, 0xF0, 0x6E, 0x3B, 0x8A, 0xD4, 0xE1,
            0x37, 0x05, 0xA8, 0x99, 0xF2, 0xDD, 0x01, 0x67
    }; // real_key ^ 0xAA

    unsigned char k[sizeof(q)];
    for (size_t i = 0; i < sizeof(q); ++i) {
        k[i] = static_cast<unsigned char>(q[i] ^ 0xAA);
    }

    // XOR-encoded versions of:
    // "gclid="
    // "gbraid="
    // "gad_source="
    // "apps.facebook.com"
    // "apps.instagram.com"
    //"solarengine"
    static const char *h2[] = {
            "745436adf51d",     // gclid=
            "745528a5f84443", // gbraid=
            "74563e9be24f0b39feca3f",     // gad_source=
            "72472ab7bf461f28f8cd6d5c3359c8a27e",     // apps.facebook.com
            "72472ab7bf491038e9ce6541391a85ae7c5a",    // apps.instagram.com
            "605836a5e345102cf4c167" // solarengine
    };

    const size_t HN2 = sizeof(h2) / sizeof(h2[0]);

    for (size_t idx = 0; idx < HN2; ++idx) {
        const char *s0 = h2[idx];
        size_t L = std::strlen(s0);
        if ((L & 1u) != 0u) continue;

        size_t n = L / 2;
        std::string d;
        d.resize(n);

        bool bad = false;
        for (size_t j = 0, z = 0; j < L; j += 2, ++z) {
            int hi = hx(s0[j]);
            int lo = hx(s0[j + 1]);
            if (hi < 0 || lo < 0) {
                bad = true;
                break;
            }
            unsigned char b = static_cast<unsigned char>((hi << 4) | lo);
            unsigned char v = static_cast<unsigned char>(b ^ k[z % sizeof(k)]);
            d[z] = static_cast<char>(v);
        }
        if (bad) continue;

        if (!d.empty() && r.find(d) != std::string::npos) {
            return JNI_TRUE;
        }
    }

    return JNI_FALSE;
}

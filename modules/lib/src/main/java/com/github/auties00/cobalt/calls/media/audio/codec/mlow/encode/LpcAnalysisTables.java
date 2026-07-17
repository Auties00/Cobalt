package com.github.auties00.cobalt.calls.media.audio.codec.mlow.encode;

/**
 * Holds the constant tables for the MLow encoder LPC analysis stage.
 *
 * <p>Two families of tables live here: the brute force inverse DCT cosine rows ({@code Cdif},
 * {@code Csumdiff}, {@code Csumsum}) packed into {@link #DCT}, and the three analysis windows
 * ({@link #WIN1_20MS} leading sine taper, {@link #WIN3_LONG} and {@link #WIN3_SHORT} trailing cosine tapers).
 * Every value is a pure constant of the codec build, independent of the audio signal, so the tables are
 * fixed data rather than something computed at runtime. Scope is the SMPL 16 kHz, 60 ms, mono path:
 * prediction order {@code 16}, FFT length {@code 512}, and 20 ms frames.
 *
 * @implNote This implementation stores the exact table bytes as hexadecimal float literals rather than
 * recomputing them from {@code cos}/{@code sin} at startup. The rows are {@code cos(omega) * scale} and the
 * tapers are {@code sin}/{@code cos} of a fixed angle, but the C runtime transcendental functions that
 * produced the reference tables are not correctly rounded and disagree with the JDK {@link Math}
 * functions by up to one unit in the last place on a few entries. Those single bit differences are harmless
 * in isolation, but the brute DCT dot product that recovers the autocorrelation has heavy cancellation at
 * the higher lags, which amplifies a one bit table error into a part in {@code 1e7} autocorrelation error
 * and then, through the ill conditioned Levinson recursion, into a line spectral frequency shift large
 * enough to flip a quantizer index on resonant frames. Storing the literals keeps the bytes bit for bit
 * identical to the reference and removes that source of drift entirely.
 */
final class LpcAnalysisTables {
    /**
     * Flat brute DCT cosine table of {@code 2048} entries: eight {@code Cdif} rows, then four
     * {@code Csumdiff} rows, then four {@code Csumsum} rows, each row {@code 128} {@code double} entries.
     */
    static final double[] DCT = dct();

    /**
     * Assembles {@link #DCT} by filling each of its sixteen row blocks in turn.
     *
     * @return the flat brute DCT cosine table
     */
    private static double[] dct() {
        var t = new double[2048];
        dct0(t);
        dct1(t);
        dct2(t);
        dct3(t);
        dct4(t);
        dct5(t);
        dct6(t);
        dct7(t);
        return t;
    }

    /**
     * Fills {@link #DCT} entries 0 through 255.
     *
     * @param t the table being assembled
     */
    private static void dct0(double[] t) {
        var v = new double[]{
                0x1.0p-8d, 0x1.fff62169aff69p-9d, 0x1.ffd8860827f4fp-9d, 0x1.ffa72eff9c89dp-9d, 0x1.ff621e370373ep-9d,
                0x1.ff09565800dbep-9d, 0x1.fe9cdacecd0d4p-9d, 0x1.fe1cafca12aep-9d, 0x1.fd88da3ac5782p-9d, 0x1.fce15fd3f174fp-9d,
                0x1.fc26470a82bd2p-9d, 0x1.fb57971505bf5p-9d, 0x1.fa7557eb600f8p-9d, 0x1.f97f924681c25p-9d, 0x1.f8764fa00f574p-9d,
                0x1.f7599a320434bp-9d, 0x1.f6297cf64db9ap-9d, 0x1.f4e603a65ee9ap-9d, 0x1.f38f3ababcb58p-9d, 0x1.f2252f6a82e74p-9d,
                0x1.f0a7efaae1b44p-9d, 0x1.ef178a2e93fcp-9d, 0x1.ed740e654e378p-9d, 0x1.ebbd8c7b26209p-9d, 0x1.e9f41557f314ep-9d,
                0x1.e817ba9ea73c8p-9d, 0x1.e6288eaca179fp-9d, 0x1.e426a498f829dp-9d, 0x1.e2121033bcb9fp-9d, 0x1.dfeae605381ebp-9d,
                0x1.ddb13b4d202e1p-9d, 0x1.db652601c5e8bp-9d, 0x1.d906bccf3cb87p-9d, 0x1.d69617167aad3p-9d, 0x1.d4134cec71c06p-9d,
                0x1.d17e771922283p-9d, 0x1.ced7af16a5c38p-9d, 0x1.cc1f0f1034a86p-9d, 0x1.c954b1e122dddp-9d, 0x1.c678b313d74c4p-9d,
                0x1.c38b2ee0bbed9p-9d, 0x1.c08c422d2747cp-9d, 0x1.bd7c0a8a3f3d4p-9d, 0x1.ba5aa633d53d6p-9d, 0x1.b728340f3bep-9d,
                0x1.b3e4d3aa15f91p-9d, 0x1.b090a5391f2e4p-9d, 0x1.ad2bc996ee1bep-9d, 0x1.a9b66242b014ap-9d, 0x1.a630915ede892p-9d,
                0x1.a29a79afee23ap-9d, 0x1.9ef43e9af7a53p-9d, 0x1.9b3e04245a916p-9d, 0x1.9777eeee59b5fp-9d, 0x1.93a22437b19c1p-9d,
                0x1.8fbcc9da28f1fp-9d, 0x1.8bc806491af94p-9d, 0x1.87c4008ffc0a6p-9d, 0x1.83b0e050d83adp-9d, 0x1.7f8ecdc2cc348p-9d,
                0x1.7b5df1b0784edp-9d, 0x1.771e75766df6ep-9d, 0x1.72d0830197784p-9d, 0x1.6e7444cd9a345p-9d, 0x1.6a09e5e333598p-9d,
                0x1.659191d68f29cp-9d, 0x1.610b74c59ae0dp-9d, 0x1.5c77bb56514bp-9d, 0x1.57d692b5021d8p-9d, 0x1.5328289294205p-9d,
                0x1.4e6cab22c23bfp-9d, 0x1.49a4491a537c1p-9d, 0x1.44cf31ad4e182p-9d, 0x1.3fed948d25944p-9d, 0x1.3affa1e6e40cbp-9d,
                0x1.36058a614ebc7p-9d, 0x1.30ff7f1b05d3ep-9d, 0x1.2bedb1a89faf2p-9d, 0x1.26d05412bf811p-9d, 0x1.21a798d42784dp-9d,
                0x1.1c73b2d7c6c89p-9d, 0x1.1734d576c2a5dp-9d, 0x1.11eb34767bf96p-9d, 0x1.0c970406902f7p-9d, 0x1.073878bed6376p-9d,
                0x1.01cfc79d57723p-9d, 0x1.f8ba4c0889621p-10d, 0x1.edc1936fceae4p-10d, 0x1.e2b5d1b91d662p-10d, 0x1.d79773e8f349fp-10d,
                0x1.cc66e7bb794aap-10d, 0x1.c1249ba048828p-10d, 0x1.b5d0feb62843bp-10d, 0x1.aa6c80c6c564ep-10d, 0x1.9ef7924262f7ap-10d,
                0x1.9372a43b8492fp-10d, 0x1.87de2862925c2p-10d, 0x1.7c3a910176f92p-10d, 0x1.708850f737987p-10d, 0x1.64c7dbb38638cp-10d,
                0x1.58f9a5324e5d6p-10d, 0x1.4d1e21f73c5a1p-10d, 0x1.4135c7093f63bp-10d, 0x1.354109ee0690dp-10d, 0x1.294060a578f7bp-10d,
                0x1.1d3441a529163p-10d, 0x1.111d23d3c3afbp-10d, 0x1.04fb7e847a4f8p-10d, 0x1.f19f92e4d337ap-11d, 0x1.d934f977f74e2p-11d,
                0x1.c0b821bc8cdabp-11d, 0x1.a829fd60cd57p-11d, 0x1.8f8b7ebdae7afp-11d, 0x1.76dd98cd8a64bp-11d, 0x1.5e213f22c187bp-11d,
                0x1.455765de56bf8p-11d, 0x1.2c8101a685e28p-11d, 0x1.139f079d5532p-11d, 0x1.f564daae44098p-12d, 0x1.c37851a252117p-12d,
                0x1.917a60d014f3ap-12d, 0x1.5f6cf59c9556dp-12d, 0x1.2d51fe059e842p-12d, 0x1.f656d11d65ecep-13d, 0x1.91f6485bf7853p-13d,
                0x1.2d8640726727fp-13d, 0x1.9215314a37e5fp-14d, 0x1.921cc2acde02dp-15d, 0x1.0p-8d, 0x1.ffa72eff9c89dp-9d,
                0x1.fe9cdacecd0d4p-9d, 0x1.fce15fd3f174fp-9d, 0x1.fa7557eb600f8p-9d, 0x1.f7599a320434bp-9d, 0x1.f38f3ababcb58p-9d,
                0x1.ef178a2e93fcp-9d, 0x1.e9f41557f314ep-9d, 0x1.e426a498f829dp-9d, 0x1.ddb13b4d202e1p-9d, 0x1.d69617167aad3p-9d,
                0x1.ced7af16a5c38p-9d, 0x1.c678b313d74c4p-9d, 0x1.bd7c0a8a3f3d4p-9d, 0x1.b3e4d3aa15f91p-9d, 0x1.a9b66242b014ap-9d,
                0x1.9ef43e9af7a53p-9d, 0x1.93a22437b19c1p-9d, 0x1.87c4008ffc0a6p-9d, 0x1.7b5df1b0784edp-9d, 0x1.6e7444cd9a345p-9d,
                0x1.610b74c59ae0dp-9d, 0x1.5328289294205p-9d, 0x1.44cf31ad4e182p-9d, 0x1.36058a614ebc7p-9d, 0x1.26d05412bf811p-9d,
                0x1.1734d576c2a5dp-9d, 0x1.073878bed6376p-9d, 0x1.edc1936fceae4p-10d, 0x1.cc66e7bb794aap-10d, 0x1.aa6c80c6c564ep-10d,
                0x1.87de2862925c2p-10d, 0x1.64c7dbb38638cp-10d, 0x1.4135c7093f63bp-10d, 0x1.1d3441a529163p-10d, 0x1.f19f92e4d337ap-11d,
                0x1.a829fd60cd57p-11d, 0x1.5e213f22c187bp-11d, 0x1.139f079d5532p-11d, 0x1.917a60d014f3ap-12d, 0x1.f656d11d65ecep-13d,
                0x1.9215314a37e5fp-14d, -0x1.921d7e666e0cfp-15d, -0x1.91f6773cca10dp-13d, -0x1.5f6d0cfe141a3p-12d, -0x1.f564f1f8bbb04p-12d,
                -0x1.4557717404b61p-11d, -0x1.8f8b8a3fc9abbp-11d, -0x1.d93504e2819a7p-11d, -0x1.111d297b456aep-10d, -0x1.35410f85ceaeap-10d,
                -0x1.58f9aab86c203p-10d, -0x1.7c3a9673ffc43p-10d, -0x1.9ef7979f72f95p-10d, -0x1.c124a0e6035c7p-10d, -0x1.e2b5d6e5aed21p-10d,
                -0x1.01cfca2625ab7p-9d, -0x1.11eb36f0ee9d6p-9d, -0x1.21a79b3f62763p-9d, -0x1.30ff8176323cdp-9d, -0x1.3fed96d772312p-9d,
                -0x1.4e6cad5b63a3ap-9d, -0x1.5c77bd7c8235dp-9d, -0x1.6a09e7f634e57p-9d, -0x1.771e777587e9fp-9d, -0x1.83b0e23b5942bp-9d,
                -0x1.8fbccbaf66e1fp-9d, -0x1.9b3e05e3b29d2p-9d, -0x1.a6309307b57d1p-9d, -0x1.b090a6cae1a5bp-9d, -0x1.ba5aa7adf7d56p-9d,
                -0x1.c38b3042bb75ap-9d, -0x1.cc1f1059964fap-9d, -0x1.d4134e1cc3405p-9d, -0x1.db6527189dacep-9d, -0x1.e2121130ba049p-9d,
                -0x1.e817bb817247fp-9d, -0x1.ed740f2d9854dp-9d, -0x1.f225301806996p-9d, -0x1.f6297d88cecdbp-9d, -0x1.f97f92bdcd645p-9d,
                -0x1.fc2647666f89bp-9d, -0x1.fe1cb00a80c0bp-9d, -0x1.ff621e5bdc723p-9d, -0x1.fff62172e717ep-9d, -0x1.ffd885f5ba06dp-9d,
                -0x1.ff095629f4431p-9d, -0x1.fd88d9f12a2eap-9d, -0x1.fb5796aff54e9p-9d, -0x1.f8764f1faccf7p-9d, -0x1.f4e6030ad6d56p-9d,
                -0x1.f0a7eef46a096p-9d, -0x1.ebbd8ba9fe2d2p-9d, -0x1.e6288dc111ce7p-9d, -0x1.dfeae4ff9274dp-9d, -0x1.d906bbafdbd6dp-9d,
                -0x1.d17e75e069c2bp-9d, -0x1.c954b08f7f732p-9d, -0x1.c08c40c30dfbbp-9d, -0x1.b728328d2a52fp-9d, -0x1.ad2bc7fd6a3fp-9d,
                -0x1.a29a77ff860aap-9d, -0x1.9777ed27a3659p-9d, -0x1.8bc8046cb4337p-9d, -0x1.7f8ecbd15a419p-9d, -0x1.72d080fbc6ed6p-9d,
                -0x1.65918fbd13ad8p-9d, -0x1.57d6908896291p-9d, -0x1.49a446dbb81adp-9d, -0x1.3aff9f96e0992p-9d, -0x1.2bedaf48018dcp-9d,
                -0x1.1c73b0676120cp-9d, -0x1.0c9701873ba1dp-9d, -0x1.f8ba46edbe193p-10d, -0x1.d7976eb3cb344p-10d, -0x1.b5d0f96871e76p-10d,
                -0x1.93729ed716fb1p-10d, -0x1.70884b7df1b1cp-10d, -0x1.4d1e1c6b044cap-10d, -0x1.29405b083b7e3p-10d, -0x1.04fb78d82a0c7p-10d,
                -0x1.c0b81649b67c2p-11d, -0x1.76dd8d4477001p-11d, -0x1.2c80f60b36017p-11d, -0x1.c3783a4f4711p-12d, -0x1.2d51e69e3fdcp-12d,
                -0x1.2d86118b3fd03p-13d,
        };
        System.arraycopy(v, 0, t, 0, v.length);
    }

    /**
     * Fills {@link #DCT} entries 256 through 511.
     *
     * @param t the table being assembled
     */
    private static void dct1(double[] t) {
        var v = new double[]{
                0x1.0p-8d, 0x1.ff09565800dbep-9d, 0x1.fc26470a82bd2p-9d, 0x1.f7599a320434bp-9d, 0x1.f0a7efaae1b44p-9d,
                0x1.e817ba9ea73c8p-9d, 0x1.ddb13b4d202e1p-9d, 0x1.d17e771922283p-9d, 0x1.c38b2ee0bbed9p-9d, 0x1.b3e4d3aa15f91p-9d,
                0x1.a29a79afee23ap-9d, 0x1.8fbcc9da28f1fp-9d, 0x1.7b5df1b0784edp-9d, 0x1.659191d68f29cp-9d, 0x1.4e6cab22c23bfp-9d,
                0x1.36058a614ebc7p-9d, 0x1.1c73b2d7c6c89p-9d, 0x1.01cfc79d57723p-9d, 0x1.cc66e7bb794aap-10d, 0x1.9372a43b8492fp-10d,
                0x1.58f9a5324e5d6p-10d, 0x1.1d3441a529163p-10d, 0x1.c0b821bc8cdabp-11d, 0x1.455765de56bf8p-11d, 0x1.917a60d014f3ap-12d,
                0x1.2d8640726727fp-13d, -0x1.92158f219252bp-14d, -0x1.5f6d0cfe141a3p-12d, -0x1.2c810d41d5c1p-11d, -0x1.a82a08db7ec27p-11d,
                -0x1.111d297b456aep-10d, -0x1.4d1e27837464cp-10d, -0x1.87de2dce284b4p-10d, -0x1.c124a0e6035c7p-10d, -0x1.f8ba512354a6cp-10d,
                -0x1.1734d7ec3aecdp-9d, -0x1.30ff8176323cdp-9d, -0x1.49a44b58eeda9p-9d, -0x1.610b76e57b8ecp-9d, -0x1.771e777587e9fp-9d,
                -0x1.8bc8082581bbbp-9d, -0x1.9ef44052e030ap-9d, -0x1.b090a6cae1a5bp-9d, -0x1.c08c4397409p-9d, -0x1.ced7b057b8f86p-9d,
                -0x1.db6527189dacep-9d, -0x1.e6288f9831216p-9d, -0x1.ef178aedf88e7p-9d, -0x1.f6297d88cecdbp-9d, -0x1.fb57977a162bdp-9d,
                -0x1.fe9cdb060b717p-9d, -0x1.fff62172e717ep-9d, -0x1.ff621e122a714p-9d, -0x1.fce15f812bcf5p-9d, -0x1.f8764f1faccf7p-9d,
                -0x1.f2252ebcff30fp-9d, -0x1.e9f4147df55fap-9d, -0x1.dfeae4ff9274dp-9d, -0x1.d4134bbc203c8p-9d, -0x1.c678b1b9ff265p-9d,
                -0x1.b728328d2a52fp-9d, -0x1.a6308fb60791bp-9d, -0x1.93a22269ae925p-9d, -0x1.7f8ecbd15a419p-9d, -0x1.6a09e3d031ca8p-9d,
                -0x1.532826600298bp-9d, -0x1.3aff9f96e0992p-9d, -0x1.21a79668ec90fp-9d, -0x1.0738763ab8684p-9d, -0x1.d7976eb3cb344p-10d,
                -0x1.9ef78ce552f26p-10d, -0x1.64c7d633b93f7p-10d, -0x1.29405b083b7e3p-10d, -0x1.d934ee0d6cfddp-11d, -0x1.5e21339327cbfp-11d,
                -0x1.c3783a4f4711p-12d, -0x1.91f6197b24f63p-13d, 0x1.921e3a1ffe13bp-15d, 0x1.2d522cd45bcccp-12d, 0x1.139f1ede53a2p-11d,
                0x1.8f8b95c1e4d9p-11d, 0x1.04fb89dd1acfp-10d, 0x1.4135d22d76848p-10d, 0x1.7c3a9be6888cp-10d, 0x1.b5d1095194f17p-10d,
                0x1.edc19db75e14cp-10d, 0x1.11eb396b613f2p-9d, 0x1.2bedb669dbea3p-9d, 0x1.44cf36364c71ep-9d, 0x1.5c77bfa2b31dbp-9d,
                0x1.72d0870d38849p-9d, 0x1.87c40456f6754p-9d, 0x1.9b3e07a30aa56p-9d, 0x1.ad2bccc9f5cadp-9d, 0x1.bd7c0d6e895fp-9d,
                0x1.cc1f11a2f7f3p-9d, 0x1.d906bf0dfe6fdp-9d, 0x1.e426a6818e82dp-9d, 0x1.ed740ff5e26dfp-9d, 0x1.f4e604dd6f058p-9d,
                0x1.fa7558c7c0594p-9d, 0x1.fe1cb04aeecf1p-9d, 0x1.ffd8862d03c46p-9d, 0x1.ffa72eff9c89dp-9d, 0x1.fd88da3ac5782p-9d,
                0x1.f97f924681c25p-9d, 0x1.f38f3ababcb58p-9d, 0x1.ebbd8c7b26209p-9d, 0x1.e2121033bcb9fp-9d, 0x1.d69617167aad3p-9d,
                0x1.c954b1e122dddp-9d, 0x1.ba5aa633d53d6p-9d, 0x1.a9b66242b014ap-9d, 0x1.9777eeee59b5fp-9d, 0x1.83b0e050d83adp-9d,
                0x1.6e7444cd9a345p-9d, 0x1.57d692b5021d8p-9d, 0x1.3fed948d25944p-9d, 0x1.26d05412bf811p-9d, 0x1.0c970406902f7p-9d,
                0x1.e2b5d1b91d662p-10d, 0x1.aa6c80c6c564ep-10d, 0x1.708850f737987p-10d, 0x1.354109ee0690dp-10d, 0x1.f19f92e4d337ap-11d,
                0x1.76dd98cd8a64bp-11d, 0x1.f564daae44098p-12d, 0x1.f656d11d65ecep-13d, 0x1.0p-8d, 0x1.fe1cafca12aep-9d,
                0x1.f8764fa00f574p-9d, 0x1.ef178a2e93fcp-9d, 0x1.e2121033bcb9fp-9d, 0x1.d17e771922283p-9d, 0x1.bd7c0a8a3f3d4p-9d,
                0x1.a630915ede892p-9d, 0x1.8bc806491af94p-9d, 0x1.6e7444cd9a345p-9d, 0x1.4e6cab22c23bfp-9d, 0x1.2bedb1a89faf2p-9d,
                0x1.073878bed6376p-9d, 0x1.c1249ba048828p-10d, 0x1.708850f737987p-10d, 0x1.1d3441a529163p-10d, 0x1.8f8b7ebdae7afp-11d,
                0x1.c37851a252117p-12d, 0x1.9215314a37e5fp-14d, -0x1.f656fff61505p-13d, -0x1.2c810d41d5c1p-11d, -0x1.d93504e2819a7p-11d,
                -0x1.4135cc9b5af57p-10d, -0x1.9372a99ff2277p-10d, -0x1.e2b5d6e5aed21p-10d, -0x1.1734d7ec3aecdp-9d, -0x1.3affa436e77d9p-9d,
                -0x1.5c77bd7c8235dp-9d, -0x1.7b5df3a8c7f8dp-9d, -0x1.9777f0b51002dp-9d, -0x1.b090a6cae1a5bp-9d, -0x1.c678b46daf6e6p-9d,
                -0x1.d906bdee9d962p-9d, -0x1.e817bb817247fp-9d, -0x1.f38f3b5f45c28p-9d, -0x1.fb57977a162bdp-9d, -0x1.ff621e5bdc723p-9d,
                -0x1.ffa72ee3f889p-9d, -0x1.fc2646ae95ec5p-9d, -0x1.f4e6030ad6d56p-9d, -0x1.e9f4147df55fap-9d, -0x1.db6524eaee208p-9d,
                -0x1.c954b08f7f732p-9d, -0x1.b3e4d220245dp-9d, -0x1.9b3e026502824p-9d, -0x1.7f8ecbd15a419p-9d, -0x1.610b72a5ba2fep-9d,
                -0x1.3fed9242d8f4cp-9d, -0x1.1c73b0676120cp-9d, -0x1.edc18e4c06f4cp-10d, -0x1.9ef78ce552f26p-10d, -0x1.4d1e1c6b044cap-10d,
                -0x1.f19f878305a19p-11d, -0x1.45575a48a8c64p-11d, -0x1.2d51e69e3fdcp-12d, 0x1.921e3a1ffe13bp-15d, 0x1.917a8f85852f1p-12d,
                0x1.76ddafdfb1247p-11d, 0x1.111d2f22c723cp-10d, 0x1.64c7e6b320227p-10d, 0x1.b5d1095194f17p-10d, 0x1.01cfccaef3e2ap-9d,
                0x1.26d058deb03a5p-9d, 0x1.49a44d978a365p-9d, 0x1.6a09ea09366e6p-9d, 0x1.87c40456f6754p-9d, 0x1.a29a7d10be4b2p-9d,
                0x1.ba5aa9281a69cp-9d, 0x1.ced7b198cc295p-9d, 0x1.dfeae81083664p-9d, 0x1.ed740ff5e26dfp-9d, 0x1.f7599b44ed14bp-9d,
                0x1.fd88dacdfbfe5p-9d, 0x1.fff6217c1e34fp-9d, 0x1.fe9cdacecd0d4p-9d, 0x1.f97f924681c25p-9d, 0x1.f0a7efaae1b44p-9d,
                0x1.e426a498f829dp-9d, 0x1.d4134cec71c06p-9d, 0x1.c08c422d2747cp-9d, 0x1.a9b66242b014ap-9d, 0x1.8fbcc9da28f1fp-9d,
                0x1.72d0830197784p-9d, 0x1.5328289294205p-9d, 0x1.30ff7f1b05d3ep-9d, 0x1.0c970406902f7p-9d, 0x1.cc66e7bb794aap-10d,
                0x1.7c3a910176f92p-10d, 0x1.294060a578f7bp-10d, 0x1.a829fd60cd57p-11d, 0x1.f564daae44098p-12d, 0x1.2d8640726727fp-13d,
                -0x1.91f6773cca10dp-13d, -0x1.139f133dd46b3p-11d, -0x1.c0b82d2f63358p-11d, -0x1.35410f85ceaeap-10d, -0x1.87de2dce284b4p-10d,
                -0x1.d797791e1b5bcp-10d, -0x1.11eb36f0ee9d6p-9d, -0x1.36058cb6f22e9p-9d, -0x1.57d694e16e0f2p-9d, -0x1.771e777587e9fp-9d,
                -0x1.93a22605b4a27p-9d, -0x1.ad2bcb3071f52p-9d, -0x1.c38b3042bb75ap-9d, -0x1.d696183e59901p-9d, -0x1.e6288f9831216p-9d,
                -0x1.f225301806996p-9d, -0x1.fa75585990368p-9d, -0x1.ff0956860d706p-9d, -0x1.ffd885f5ba06dp-9d, -0x1.fce15f812bcf5p-9d,
                -0x1.f6297c63cca16p-9d, -0x1.ebbd8ba9fe2d2p-9d, -0x1.ddb13a3edc3f6p-9d, -0x1.cc1f0dc6d2fd4p-9d, -0x1.b728328d2a52fp-9d,
                -0x1.9ef43ce30f165p-9d, -0x1.83b0de66572fap-9d, -0x1.65918fbd13ad8p-9d, -0x1.44cf2f68cee72p-9d, -0x1.21a79668ec90fp-9d,
                -0x1.f8ba46edbe193p-10d, -0x1.aa6c7b7147e1cp-10d, -0x1.58f99fac3097bp-10d, -0x1.04fb78d82a0c7p-10d, -0x1.5e21339327cbfp-11d,
                -0x1.5f6cde3b16908p-12d,
        };
        System.arraycopy(v, 0, t, 256, v.length);
    }

    /**
     * Fills {@link #DCT} entries 512 through 767.
     *
     * @param t the table being assembled
     */
    private static void dct2(double[] t) {
        var v = new double[]{
                0x1.0p-8d, 0x1.fce15fd3f174fp-9d, 0x1.f38f3ababcb58p-9d, 0x1.e426a498f829dp-9d, 0x1.ced7af16a5c38p-9d,
                0x1.b3e4d3aa15f91p-9d, 0x1.93a22437b19c1p-9d, 0x1.6e7444cd9a345p-9d, 0x1.44cf31ad4e182p-9d, 0x1.1734d576c2a5dp-9d,
                0x1.cc66e7bb794aap-10d, 0x1.64c7dbb38638cp-10d, 0x1.f19f92e4d337ap-11d, 0x1.139f079d5532p-11d, 0x1.9215314a37e5fp-14d,
                -0x1.5f6d0cfe141a3p-12d, -0x1.8f8b8a3fc9abbp-11d, -0x1.35410f85ceaeap-10d, -0x1.9ef7979f72f95p-10d, -0x1.01cfca2625ab7p-9d,
                -0x1.30ff8176323cdp-9d, -0x1.5c77bd7c8235dp-9d, -0x1.83b0e23b5942bp-9d, -0x1.a6309307b57d1p-9d, -0x1.c38b3042bb75ap-9d,
                -0x1.db6527189dacep-9d, -0x1.ed740f2d9854dp-9d, -0x1.f97f92bdcd645p-9d, -0x1.ff621e5bdc723p-9d, -0x1.ff095629f4431p-9d,
                -0x1.f8764f1faccf7p-9d, -0x1.ebbd8ba9fe2d2p-9d, -0x1.d906bbafdbd6dp-9d, -0x1.c08c40c30dfbbp-9d, -0x1.a29a77ff860aap-9d,
                -0x1.7f8ecbd15a419p-9d, -0x1.57d6908896291p-9d, -0x1.2bedaf48018dcp-9d, -0x1.f8ba46edbe193p-10d, -0x1.93729ed716fb1p-10d,
                -0x1.29405b083b7e3p-10d, -0x1.76dd8d4477001p-11d, -0x1.2d51e69e3fdcp-12d, 0x1.2d869e40b5cfcp-13d, 0x1.2c8118dd259dp-11d,
                0x1.04fb89dd1acfp-10d, 0x1.70885be9c35c9p-10d, 0x1.d7977e5343698p-10d, 0x1.1c73b7b892111p-9d, 0x1.49a44d978a365p-9d,
                0x1.72d0870d38849p-9d, 0x1.9777f27bc64c5p-9d, 0x1.b72837135eefp-9d, 0x1.d17e798a92e77p-9d, 0x1.e6289083c0c4bp-9d,
                0x1.f4e604dd6f058p-9d, 0x1.fd88dacdfbfe5p-9d, 0x1.fff62169aff69p-9d, 0x1.fc26470a82bd2p-9d, 0x1.f2252f6a82e74p-9d,
                0x1.e2121033bcb9fp-9d, 0x1.cc1f0f1034a86p-9d, 0x1.b090a5391f2e4p-9d, 0x1.8fbcc9da28f1fp-9d, 0x1.6a09e5e333598p-9d,
                0x1.3fed948d25944p-9d, 0x1.11eb34767bf96p-9d, 0x1.c1249ba048828p-10d, 0x1.58f9a5324e5d6p-10d, 0x1.d934f977f74e2p-11d,
                0x1.f564daae44098p-12d, 0x1.921cc2acde02dp-15d, -0x1.917a782acd13p-12d, -0x1.a82a08db7ec27p-11d, -0x1.4135cc9b5af57p-10d,
                -0x1.aa6c861c42e46p-10d, -0x1.07387b42f4044p-9d, -0x1.36058cb6f22e9p-9d, -0x1.610b76e57b8ecp-9d, -0x1.87c4027379417p-9d,
                -0x1.a9b663e3e5861p-9d, -0x1.c678b46daf6e6p-9d, -0x1.ddb13c5b6418cp-9d, -0x1.ef178aedf88e7p-9d, -0x1.fa75585990368p-9d,
                -0x1.ffa72f1b40865p-9d, -0x1.fe9cda978ea4cp-9d, -0x1.f75999a88fbe5p-9d, -0x1.e9f4147df55fap-9d, -0x1.d69615ee9bc66p-9d,
                -0x1.bd7c09181a26dp-9d, -0x1.9ef43ce30f165p-9d, -0x1.7b5defb828a1ap-9d, -0x1.532826600298bp-9d, -0x1.26d051acc720bp-9d,
                -0x1.edc18e4c06f4cp-10d, -0x1.87de22f6fc69bp-10d, -0x1.1d343c02adac6p-10d, -0x1.5e21339327cbfp-11d, -0x1.f656a244b6d07p-13d,
                0x1.91f6a61d9c991p-13d, 0x1.45577d09b2a9ep-11d, 0x1.111d2f22c723cp-10d, 0x1.7c3a9be6888cp-10d, 0x1.e2b5dc12403ap-10d,
                0x1.21a79daa9d653p-9d, 0x1.4e6caf9405088p-9d, 0x1.771e7974a1d9cp-9d, 0x1.9b3e07a30aa56p-9d, 0x1.ba5aa9281a69cp-9d,
                0x1.d4134f4d14bc6p-9d, 0x1.e817bc643d4f4p-9d, 0x1.f6297e1b4fdd8p-9d, 0x1.fe1cb04aeecf1p-9d, 0x1.ffd8860827f4fp-9d,
                0x1.fb57971505bf5p-9d, 0x1.f0a7efaae1b44p-9d, 0x1.dfeae605381ebp-9d, 0x1.c954b1e122dddp-9d, 0x1.ad2bc996ee1bep-9d,
                0x1.8bc806491af94p-9d, 0x1.659191d68f29cp-9d, 0x1.3affa1e6e40cbp-9d, 0x1.0c970406902f7p-9d, 0x1.b5d0feb62843bp-10d,
                0x1.4d1e21f73c5a1p-10d, 0x1.c0b821bc8cdabp-11d, 0x1.c37851a252117p-12d, 0x1.0p-8d, 0x1.fb57971505bf5p-9d,
                0x1.ed740e654e378p-9d, 0x1.d69617167aad3p-9d, 0x1.b728340f3bep-9d, 0x1.8fbcc9da28f1fp-9d, 0x1.610b74c59ae0dp-9d,
                0x1.2bedb1a89faf2p-9d, 0x1.e2b5d1b91d662p-10d, 0x1.64c7dbb38638cp-10d, 0x1.c0b821bc8cdabp-11d, 0x1.5f6cf59c9556dp-12d,
                -0x1.91f6773cca10dp-13d, -0x1.76dda4569dc62p-11d, -0x1.4135cc9b5af57p-10d, -0x1.c124a0e6035c7p-10d, -0x1.1c73b5482c6ep-9d,
                -0x1.53282ac525a51p-9d, -0x1.83b0e23b5942bp-9d, -0x1.ad2bcb3071f52p-9d, -0x1.ced7b057b8f86p-9d, -0x1.e817bb817247fp-9d,
                -0x1.f876502071daep-9d, -0x1.ffa72f1b40865p-9d, -0x1.fd88d9f12a2eap-9d, -0x1.f2252ebcff30fp-9d, -0x1.ddb13a3edc3f6p-9d,
                -0x1.c08c40c30dfbbp-9d, -0x1.9b3e026502824p-9d, -0x1.6e7442c1270b6p-9d, -0x1.3aff9f96e0992p-9d, -0x1.01cfc5148936bp-9d,
                -0x1.87de22f6fc69bp-10d, -0x1.04fb78d82a0c7p-10d, -0x1.f564c363cc5eap-12d, 0x1.921e3a1ffe13bp-15d, 0x1.2c8118dd259dp-11d,
                0x1.1d344cea1fe28p-10d, 0x1.9ef79cfc82f79p-10d, 0x1.0c9709053944p-9d, 0x1.44cf36364c71ep-9d, 0x1.771e7974a1d9cp-9d,
                0x1.a29a7d10be4b2p-9d, 0x1.c678b5c7878cap-9d, 0x1.e212122db74b2p-9d, 0x1.f4e604dd6f058p-9d, 0x1.fe9cdb3d49d15p-9d,
                0x1.ff09565800dbep-9d, 0x1.f6297cf64db9ap-9d, 0x1.e426a498f829dp-9d, 0x1.c954b1e122dddp-9d, 0x1.a630915ede892p-9d,
                0x1.7b5df1b0784edp-9d, 0x1.49a4491a537c1p-9d, 0x1.11eb34767bf96p-9d, 0x1.aa6c80c6c564ep-10d, 0x1.294060a578f7bp-10d,
                0x1.455765de56bf8p-11d, 0x1.9215314a37e5fp-14d, -0x1.c37868f55d0ep-12d, -0x1.f19f9e46a0c98p-11d, -0x1.7c3a9673ffc43p-10d,
                -0x1.f8ba512354a6cp-10d, -0x1.36058cb6f22e9p-9d, -0x1.6a09e7f634e57p-9d, -0x1.9777f0b51002dp-9d, -0x1.bd7c0bfc645p-9d,
                -0x1.db6527189dacep-9d, -0x1.f0a7f061595bp-9d, -0x1.fce16026b7165p-9d, -0x1.ffd885f5ba06dp-9d, -0x1.f97f91cf361c1p-9d,
                -0x1.e9f4147df55fap-9d, -0x1.d17e75e069c2bp-9d, -0x1.b090a3a75cb33p-9d, -0x1.87c3feac7edp-9d, -0x1.57d6908896291p-9d,
                -0x1.21a79668ec90fp-9d, -0x1.cc66e27dedf4cp-10d, -0x1.4d1e1c6b044cap-10d, -0x1.8f8b733b9346ep-11d, -0x1.f656a244b6d07p-13d,
                0x1.2d522cd45bcccp-12d, 0x1.a82a1456302a6p-11d, 0x1.58f9b03e89e01p-10d, 0x1.d7977e5343698p-10d, 0x1.26d058deb03a5p-9d,
                0x1.5c77bfa2b31dbp-9d, 0x1.8bc80a01e87adp-9d, 0x1.b3e4d6bdf9263p-9d, 0x1.d4134f4d14bc6p-9d, 0x1.ebbd8e1d75fb2p-9d,
                0x1.fa7558c7c0594p-9d, 0x1.fff6217c1e34fp-9d, 0x1.fc26470a82bd2p-9d, 0x1.ef178a2e93fcp-9d, 0x1.d906bccf3cb87p-9d,
                0x1.ba5aa633d53d6p-9d, 0x1.93a22437b19c1p-9d, 0x1.659191d68f29cp-9d, 0x1.30ff7f1b05d3ep-9d, 0x1.edc1936fceae4p-10d,
                0x1.708850f737987p-10d, 0x1.d934f977f74e2p-11d, 0x1.917a60d014f3ap-12d, -0x1.2d866f598e7d1p-13d, -0x1.5e214ab25b409p-11d,
                -0x1.35410f85ceaeap-10d, -0x1.b5d10403de9c6p-10d, -0x1.1734d7ec3aecdp-9d, -0x1.4e6cad5b63a3ap-9d, -0x1.7f8ecfb43e244p-9d,
                -0x1.a9b663e3e5861p-9d, -0x1.cc1f1059964fap-9d, -0x1.e6288f9831216p-9d, -0x1.f7599abb78a6dp-9d, -0x1.ff621e5bdc723p-9d,
                -0x1.fe1caf89a4971p-9d, -0x1.f38f3a1633a44p-9d, -0x1.dfeae4ff9274dp-9d, -0x1.c38b2d7ebc61bp-9d, -0x1.9ef43ce30f165p-9d,
                -0x1.72d080fbc6ed6p-9d, -0x1.3fed9242d8f4cp-9d, -0x1.0738763ab8684p-9d, -0x1.93729ed716fb1p-10d, -0x1.111d1e2c41f23p-10d,
                -0x1.139efbfcd5f68p-11d,
        };
        System.arraycopy(v, 0, t, 512, v.length);
    }

    /**
     * Fills {@link #DCT} entries 768 through 1023.
     *
     * @param t the table being assembled
     */
    private static void dct3(double[] t) {
        var v = new double[]{
                0x1.0p-8d, 0x1.f97f924681c25p-9d, 0x1.e6288eaca179fp-9d, 0x1.c678b313d74c4p-9d, 0x1.9b3e04245a916p-9d,
                0x1.659191d68f29cp-9d, 0x1.26d05412bf811p-9d, 0x1.c1249ba048828p-10d, 0x1.294060a578f7bp-10d, 0x1.139f079d5532p-11d,
                -0x1.92158f219252bp-14d, -0x1.76dda4569dc62p-11d, -0x1.58f9aab86c203p-10d, -0x1.edc1989396639p-10d, -0x1.3affa436e77d9p-9d,
                -0x1.771e777587e9fp-9d, -0x1.a9b663e3e5861p-9d, -0x1.d17e7851da89cp-9d, -0x1.ed740f2d9854dp-9d, -0x1.fce16026b7165p-9d,
                -0x1.ff621e122a714p-9d, -0x1.f4e6030ad6d56p-9d, -0x1.ddb13a3edc3f6p-9d, -0x1.ba5aa4b9b2a19p-9d, -0x1.8bc8046cb4337p-9d,
                -0x1.532826600298bp-9d, -0x1.11eb31fc0953p-9d, -0x1.93729ed716fb1p-10d, -0x1.f19f878305a19p-11d, -0x1.5f6cde3b16908p-12d,
                0x1.2d522cd45bcccp-12d, 0x1.d935104d0be2cp-11d, 0x1.87de3339be371p-10d, 0x1.0c9709053944p-9d, 0x1.4e6caf9405088p-9d,
                0x1.87c40456f6754p-9d, 0x1.b72837135eefp-9d, 0x1.db65282f756d2p-9d, 0x1.f38f3c03cecb6p-9d, 0x1.ff0956b41a00ap-9d,
                0x1.fd88da3ac5782p-9d, 0x1.ef178a2e93fcp-9d, 0x1.d4134cec71c06p-9d, 0x1.ad2bc996ee1bep-9d, 0x1.7b5df1b0784edp-9d,
                0x1.3fed948d25944p-9d, 0x1.f8ba4c0889621p-10d, 0x1.64c7dbb38638cp-10d, 0x1.8f8b7ebdae7afp-11d, 0x1.2d8640726727fp-13d,
                -0x1.f564f1f8bbb04p-12d, -0x1.1d344747a47d9p-10d, -0x1.b5d10403de9c6p-10d, -0x1.21a79b3f62763p-9d, -0x1.610b76e57b8ecp-9d,
                -0x1.9777f0b51002dp-9d, -0x1.c38b3042bb75ap-9d, -0x1.e426a58d43585p-9d, -0x1.f876502071daep-9d, -0x1.fff62172e717ep-9d,
                -0x1.fa75577d2fe44p-9d, -0x1.e817b9bbdc2dp-9d, -0x1.c954b08f7f732p-9d, -0x1.9ef43ce30f165p-9d, -0x1.6a09e3d031ca8p-9d,
                -0x1.2bedaf48018dcp-9d, -0x1.cc66e27dedf4cp-10d, -0x1.354104563e705p-10d, -0x1.2c80f60b36017p-11d, 0x1.921e3a1ffe13bp-15d,
                0x1.5e215641f4f68p-11d, 0x1.4d1e2d0fac6cbp-10d, 0x1.e2b5dc12403ap-10d, 0x1.36058f0c959e2p-9d, 0x1.72d0870d38849p-9d,
                0x1.a63094b08c6d7p-9d, 0x1.ced7b198cc295p-9d, 0x1.ebbd8e1d75fb2p-9d, 0x1.fc2647c25c52p-9d, 0x1.ffa72eff9c89dp-9d,
                0x1.f6297cf64db9ap-9d, 0x1.dfeae605381ebp-9d, 0x1.bd7c0a8a3f3d4p-9d, 0x1.8fbcc9da28f1fp-9d, 0x1.57d692b5021d8p-9d,
                0x1.1734d576c2a5dp-9d, 0x1.9ef7924262f7ap-10d, 0x1.04fb7e847a4f8p-10d, 0x1.917a60d014f3ap-12d, -0x1.f656fff61505p-13d,
                -0x1.c0b82d2f63358p-11d, -0x1.7c3a9673ffc43p-10d, -0x1.07387b42f4044p-9d, -0x1.49a44b58eeda9p-9d, -0x1.83b0e23b5942bp-9d,
                -0x1.b3e4d53407917p-9d, -0x1.d906bdee9d962p-9d, -0x1.f225301806996p-9d, -0x1.fe9cdb060b717p-9d, -0x1.fe1caf89a4971p-9d,
                -0x1.f0a7eef46a096p-9d, -0x1.d69615ee9bc66p-9d, -0x1.b090a3a75cb33p-9d, -0x1.7f8ecbd15a419p-9d, -0x1.44cf2f68cee72p-9d,
                -0x1.01cfc5148936bp-9d, -0x1.70884b7df1b1cp-10d, -0x1.a829f1e61be8p-11d, -0x1.91f6197b24f63p-13d, 0x1.c37880486806dp-12d,
                0x1.111d2f22c723cp-10d, 0x1.aa6c8b71c0606p-10d, 0x1.1c73b7b892111p-9d, 0x1.5c77bfa2b31dbp-9d, 0x1.93a227d3b7a57p-9d,
                0x1.c08c450159d48p-9d, 0x1.e212122db74b2p-9d, 0x1.f7599b44ed14bp-9d, 0x1.ffd8862d03c46p-9d, 0x1.fb57971505bf5p-9d,
                0x1.e9f41557f314ep-9d, 0x1.cc1f0f1034a86p-9d, 0x1.a29a79afee23ap-9d, 0x1.6e7444cd9a345p-9d, 0x1.30ff7f1b05d3ep-9d,
                0x1.d79773e8f349fp-10d, 0x1.4135c7093f63bp-10d, 0x1.455765de56bf8p-11d, 0x1.0p-8d, 0x1.f7599a320434bp-9d,
                0x1.ddb13b4d202e1p-9d, 0x1.b3e4d3aa15f91p-9d, 0x1.7b5df1b0784edp-9d, 0x1.36058a614ebc7p-9d, 0x1.cc66e7bb794aap-10d,
                0x1.1d3441a529163p-10d, 0x1.917a60d014f3ap-12d, -0x1.5f6d0cfe141a3p-12d, -0x1.111d297b456aep-10d, -0x1.c124a0e6035c7p-10d,
                -0x1.30ff8176323cdp-9d, -0x1.771e777587e9fp-9d, -0x1.b090a6cae1a5bp-9d, -0x1.db6527189dacep-9d, -0x1.f6297d88cecdbp-9d,
                -0x1.fff62172e717ep-9d, -0x1.f8764f1faccf7p-9d, -0x1.dfeae4ff9274dp-9d, -0x1.b728328d2a52fp-9d, -0x1.7f8ecbd15a419p-9d,
                -0x1.3aff9f96e0992p-9d, -0x1.d7976eb3cb344p-10d, -0x1.29405b083b7e3p-10d, -0x1.c3783a4f4711p-12d, 0x1.2d522cd45bcccp-12d,
                0x1.04fb89dd1acfp-10d, 0x1.b5d1095194f17p-10d, 0x1.2bedb669dbea3p-9d, 0x1.72d0870d38849p-9d, 0x1.ad2bccc9f5cadp-9d,
                0x1.d906bf0dfe6fdp-9d, 0x1.f4e604dd6f058p-9d, 0x1.ffd8862d03c46p-9d, 0x1.f97f924681c25p-9d, 0x1.e2121033bcb9fp-9d,
                0x1.ba5aa633d53d6p-9d, 0x1.83b0e050d83adp-9d, 0x1.3fed948d25944p-9d, 0x1.e2b5d1b91d662p-10d, 0x1.354109ee0690dp-10d,
                0x1.f564daae44098p-12d, -0x1.f656fff61505p-13d, -0x1.f19f9e46a0c98p-11d, -0x1.aa6c861c42e46p-10d, -0x1.26d05678b7defp-9d,
                -0x1.6e7446da0d5a3p-9d, -0x1.a9b663e3e5861p-9d, -0x1.d696183e59901p-9d, -0x1.f38f3b5f45c28p-9d, -0x1.ffa72f1b40865p-9d,
                -0x1.fa75577d2fe44p-9d, -0x1.e426a3a4acf73p-9d, -0x1.bd7c09181a26dp-9d, -0x1.87c3feac7edp-9d, -0x1.44cf2f68cee72p-9d,
                -0x1.edc18e4c06f4cp-10d, -0x1.4135c17723cf4p-10d, -0x1.139efbfcd5f68p-11d, 0x1.91f6a61d9c991p-13d, 0x1.d935104d0be2cp-11d,
                0x1.9ef79cfc82f79p-10d, 0x1.21a79daa9d653p-9d, 0x1.6a09ea09366e6p-9d, 0x1.a63094b08c6d7p-9d, 0x1.d4134f4d14bc6p-9d,
                0x1.f22530c58a475p-9d, 0x1.ff621e80b56c4p-9d, 0x1.fb57971505bf5p-9d, 0x1.e6288eaca179fp-9d, 0x1.c08c422d2747cp-9d,
                0x1.8bc806491af94p-9d, 0x1.49a4491a537c1p-9d, 0x1.f8ba4c0889621p-10d, 0x1.4d1e21f73c5a1p-10d, 0x1.2c8101a685e28p-11d,
                -0x1.2d866f598e7d1p-13d, -0x1.c0b82d2f63358p-11d, -0x1.9372a99ff2277p-10d, -0x1.1c73b5482c6ep-9d, -0x1.659193f00aa3p-9d,
                -0x1.a29a7b6056392p-9d, -0x1.d17e7851da89cp-9d, -0x1.f0a7f061595bp-9d, -0x1.ff0956860d706p-9d, -0x1.fc2646ae95ec5p-9d,
                -0x1.e817b9bbdc2dp-9d, -0x1.c38b2d7ebc61bp-9d, -0x1.8fbcc804eafe9p-9d, -0x1.4e6ca8ea20d17p-9d, -0x1.01cfc5148936bp-9d,
                -0x1.58f99fac3097bp-10d, -0x1.45575a48a8c64p-11d, 0x1.9215ecf8ecbcp-14d, 0x1.a82a1456302a6p-11d, 0x1.87de3339be371p-10d,
                0x1.1734da61b3318p-9d, 0x1.610b79055c39bp-9d, 0x1.9ef4420ac8b8ap-9d, 0x1.ced7b198cc295p-9d, 0x1.ef178bad5d1ccp-9d,
                0x1.fe9cdb3d49d15p-9d, 0x1.fce15fd3f174fp-9d, 0x1.e9f41557f314ep-9d, 0x1.c678b313d74c4p-9d, 0x1.93a22437b19c1p-9d,
                0x1.5328289294205p-9d, 0x1.073878bed6376p-9d, 0x1.64c7dbb38638cp-10d, 0x1.5e213f22c187bp-11d, -0x1.921d7e666e0cfp-15d,
                -0x1.8f8b8a3fc9abbp-11d, -0x1.7c3a9673ffc43p-10d, -0x1.11eb36f0ee9d6p-9d, -0x1.5c77bd7c8235dp-9d, -0x1.9b3e05e3b29d2p-9d,
                -0x1.cc1f1059964fap-9d, -0x1.ed740f2d9854dp-9d, -0x1.fe1cb00a80c0bp-9d, -0x1.fd88d9f12a2eap-9d, -0x1.ebbd8ba9fe2d2p-9d,
                -0x1.c954b08f7f732p-9d, -0x1.9777ed27a3659p-9d, -0x1.57d6908896291p-9d, -0x1.0c9701873ba1dp-9d, -0x1.70884b7df1b1cp-10d,
                -0x1.76dd8d4477001p-11d,
        };
        System.arraycopy(v, 0, t, 768, v.length);
    }

    /**
     * Fills {@link #DCT} entries 1024 through 1279.
     *
     * @param t the table being assembled
     */
    private static void dct4(double[] t) {
        var v = new double[]{
                0x1.0p-9d, 0x1.ffd8860827f4fp-10d, 0x1.ff621e370373ep-10d, 0x1.fe9cdacecd0d4p-10d, 0x1.fd88da3ac5782p-10d,
                0x1.fc26470a82bd2p-10d, 0x1.fa7557eb600f8p-10d, 0x1.f8764fa00f574p-10d, 0x1.f6297cf64db9ap-10d, 0x1.f38f3ababcb58p-10d,
                0x1.f0a7efaae1b44p-10d, 0x1.ed740e654e378p-10d, 0x1.e9f41557f314ep-10d, 0x1.e6288eaca179fp-10d, 0x1.e2121033bcb9fp-10d,
                0x1.ddb13b4d202e1p-10d, 0x1.d906bccf3cb87p-10d, 0x1.d4134cec71c06p-10d, 0x1.ced7af16a5c38p-10d, 0x1.c954b1e122dddp-10d,
                0x1.c38b2ee0bbed9p-10d, 0x1.bd7c0a8a3f3d4p-10d, 0x1.b728340f3bep-10d, 0x1.b090a5391f2e4p-10d, 0x1.a9b66242b014ap-10d,
                0x1.a29a79afee23ap-10d, 0x1.9b3e04245a916p-10d, 0x1.93a22437b19c1p-10d, 0x1.8bc806491af94p-10d, 0x1.83b0e050d83adp-10d,
                0x1.7b5df1b0784edp-10d, 0x1.72d0830197784p-10d, 0x1.6a09e5e333598p-10d, 0x1.610b74c59ae0dp-10d, 0x1.57d692b5021d8p-10d,
                0x1.4e6cab22c23bfp-10d, 0x1.44cf31ad4e182p-10d, 0x1.3affa1e6e40cbp-10d, 0x1.30ff7f1b05d3ep-10d, 0x1.26d05412bf811p-10d,
                0x1.1c73b2d7c6c89p-10d, 0x1.11eb34767bf96p-10d, 0x1.073878bed6376p-10d, 0x1.f8ba4c0889621p-11d, 0x1.e2b5d1b91d662p-11d,
                0x1.cc66e7bb794aap-11d, 0x1.b5d0feb62843bp-11d, 0x1.9ef7924262f7ap-11d, 0x1.87de2862925c2p-11d, 0x1.708850f737987p-11d,
                0x1.58f9a5324e5d6p-11d, 0x1.4135c7093f63bp-11d, 0x1.294060a578f7bp-11d, 0x1.111d23d3c3afbp-11d, 0x1.f19f92e4d337ap-12d,
                0x1.c0b821bc8cdabp-12d, 0x1.8f8b7ebdae7afp-12d, 0x1.5e213f22c187bp-12d, 0x1.2c8101a685e28p-12d, 0x1.f564daae44098p-13d,
                0x1.917a60d014f3ap-13d, 0x1.2d51fe059e842p-13d, 0x1.91f6485bf7853p-14d, 0x1.9215314a37e5fp-15d, -0x1.777a5cf72ceccp-34d,
                -0x1.92158f219252bp-15d, -0x1.91f6773cca10dp-14d, -0x1.2d52156cfd29bp-13d, -0x1.917a782acd13p-13d, -0x1.f564f1f8bbb04p-13d,
                -0x1.2c810d41d5c1p-12d, -0x1.5e214ab25b409p-12d, -0x1.8f8b8a3fc9abbp-12d, -0x1.c0b82d2f63358p-12d, -0x1.f19f9e46a0c98p-12d,
                -0x1.111d297b456aep-11d, -0x1.29406642b66ecp-11d, -0x1.4135cc9b5af57p-11d, -0x1.58f9aab86c203p-11d, -0x1.708856707d7cp-11d,
                -0x1.87de2dce284b4p-11d, -0x1.9ef7979f72f95p-11d, -0x1.b5d10403de9c6p-11d, -0x1.cc66ecf9049cap-11d, -0x1.e2b5d6e5aed21p-11d,
                -0x1.f8ba512354a6cp-11d, -0x1.07387b42f4044p-10d, -0x1.11eb36f0ee9d6p-10d, -0x1.1c73b5482c6ep-10d, -0x1.26d05678b7defp-10d,
                -0x1.30ff8176323cdp-10d, -0x1.3affa436e77d9p-10d, -0x1.44cf33f1cd466p-10d, -0x1.4e6cad5b63a3ap-10d, -0x1.57d694e16e0f2p-10d,
                -0x1.610b76e57b8ecp-10d, -0x1.6a09e7f634e57p-10d, -0x1.72d0850767fffp-10d, -0x1.7b5df3a8c7f8dp-10d, -0x1.83b0e23b5942bp-10d,
                -0x1.8bc8082581bbbp-10d, -0x1.93a22605b4a27p-10d, -0x1.9b3e05e3b29d2p-10d, -0x1.a29a7b6056392p-10d, -0x1.a9b663e3e5861p-10d,
                -0x1.b090a6cae1a5bp-10d, -0x1.b72835914d696p-10d, -0x1.bd7c0bfc645p-10d, -0x1.c38b3042bb75ap-10d, -0x1.c954b332c644ap-10d,
                -0x1.ced7b057b8f86p-10d, -0x1.d4134e1cc3405p-10d, -0x1.d906bdee9d962p-10d, -0x1.ddb13c5b6418cp-10d, -0x1.e2121130ba049p-10d,
                -0x1.e6288f9831216p-10d, -0x1.e9f41631f0c5fp-10d, -0x1.ed740f2d9854dp-10d, -0x1.f0a7f061595bp-10d, -0x1.f38f3b5f45c28p-10d,
                -0x1.f6297d88cecdbp-10d, -0x1.f876502071daep-10d, -0x1.fa75585990368p-10d, -0x1.fc2647666f89bp-10d, -0x1.fd88da8460bd6p-10d,
                -0x1.fe9cdb060b717p-10d, -0x1.ff621e5bdc723p-10d, -0x1.ffd8861a95dedp-10d, 0x1.0p-9d, 0x1.fe9cdacffa5f4p-10d,
                0x1.fa7557f01213ap-10d, 0x1.f38f3ac541067p-10d, 0x1.e9f4156a871b1p-10d, 0x1.ddb13b69ead49p-10d, 0x1.ced7af3fb15bap-10d,
                0x1.bd7c0ac1739ddp-10d, 0x1.a9b66289cd01ap-10d, 0x1.93a224904981ep-10d, 0x1.7b5df21beb5cep-10d, 0x1.610b7545128e2p-10d,
                0x1.44cf3241b91abp-10d, 0x1.26d054bcce42cp-10d, 0x1.0738797ef7ce6p-10d, 0x1.cc66e96838b1dp-11d, 0x1.87de2a3b99195p-11d,
                0x1.4135c90dca7b9p-11d, 0x1.f19f97424d136p-12d, 0x1.5e2143d0da853p-12d, 0x1.917a6ac3c1373p-13d, 0x1.92155b46fae5p-15d,
                -0x1.91f6614392c4ep-14d, -0x1.f564e68edf984p-13d, -0x1.8f8b845d4d353p-12d, -0x1.111d26784014ap-11d, -0x1.58f9a7a90ec63p-11d,
                -0x1.9ef794899267fp-11d, -0x1.e2b5d3cf7b408p-11d, -0x1.11eb3568eb87fp-10d, -0x1.30ff7ff4a8194p-10d, -0x1.4e6cabe3d04aap-10d,
                -0x1.6a09e68c2affep-10d, -0x1.83b0e0e279329p-10d, -0x1.9b3e049fa3266p-10d, -0x1.b090a59f4864ap-10d, -0x1.c38b2f33353p-10d,
                -0x1.d4134d2cdc202p-10d, -0x1.e2121063e5cadp-10d, -0x1.ed740e872a934p-10d, -0x1.f6297d0bf3366p-10d, -0x1.fc2647162262fp-10d,
                -0x1.ff621e3ae33e7p-10d, -0x1.ffd886069c6ecp-10d, -0x1.fd88da362b3b6p-10d, -0x1.f8764f9ac483cp-10d, -0x1.f0a7efa73f665p-10d,
                -0x1.e6288eacf550bp-10d, -0x1.d906bcd5c2792p-10d, -0x1.c954b1effe407p-10d, -0x1.b728342872936p-10d, -0x1.a29a79d562144p-10d,
                -0x1.8bc8067c84e7ap-10d, -0x1.72d0834481ea1p-10d, -0x1.57d69308c4b4p-10d, -0x1.3affa24c9f4a3p-10d, -0x1.1c73b350604f4p-10d,
                -0x1.f8ba4d20c8035p-11d, -0x1.b5d0fff63fc61p-11d, -0x1.7088525f6f3fdp-11d, -0x1.294062358ed0fp-11d, -0x1.c0b8252adaa02p-12d,
                -0x1.2c810560436dap-12d, -0x1.2d52060866551p-13d, 0x1.99bc5b961b1adp-36d, 0x1.2d520c6ae6ecp-13d, 0x1.2c81088add8a8p-12d,
                0x1.c0b8284a6a04fp-12d, 0x1.294063bda68b5p-11d, 0x1.708853ddb7104p-11d, 0x1.b5d10168a5579p-11d, 0x1.f8ba4e85497d3p-11d,
                0x1.1c73b3fab7b6bp-10d, 0x1.3affa2ee210c3p-10d, 0x1.57d693a090c22p-10d, 0x1.72d083d1c5ae6p-10d, 0x1.8bc806fe7c689p-10d,
                0x1.a29a7a4b5904cp-10d, 0x1.b7283491c54cdp-10d, 0x1.c954b24c1aa5ep-10d, 0x1.d906bd2428c1cp-10d, 0x1.e6288eed38b94p-10d,
                0x1.f0a7efd906c8p-10d, 0x1.f8764fbdcacffp-10d, 0x1.fd88da4a3fdb3p-10d, 0x1.ffd8860ba386ap-10d, 0x1.ff621e7a87cecp-10d,
                0x1.fc2647b4e8001p-10d, 0x1.f6297e08fd9d5p-10d, 0x1.ed740fe11ab9dp-10d, 0x1.e2121218dbc6bp-10d, 0x1.d4134f3a79c0cp-10d,
                0x1.c38b3196a1477p-10d, 0x1.b090a85532bb3p-10d, 0x1.9b3e07a449135p-10d, 0x1.83b0e431aad37p-10d, 0x1.6a09ea215107fp-10d,
                0x1.4e6cafb9f260dp-10d, 0x1.30ff840673bdap-10d, 0x1.11eb39b0bb75dp-10d, 0x1.e2b5dcbf43464p-11d, 0x1.9ef79dcd1cc8ep-11d,
                0x1.58f9b1338180fp-11d, 0x1.111d303c5ec95p-11d, 0x1.8f8b983dc9cefp-12d, 0x1.f5650ec930cc8p-13d, 0x1.91f6b23b47c1cp-14d,
                -0x1.9214b93212485p-15d, -0x1.917a426d5e09cp-13d, -0x1.5e212fd90f3f3p-12d, -0x1.f19f83999ba38p-12d, -0x1.4135bf6ea1934p-11d,
                -0x1.87de20def91c2p-11d, -0x1.cc66e05b1e6ddp-11d, -0x1.0738752674c7p-10d, -0x1.26d050985cb53p-10d, -0x1.44cf2e57182aap-10d,
                -0x1.610b7199b12a4p-10d, -0x1.7b5deeb4e0b5cp-10d, -0x1.93a221724df9dp-10d, -0x1.a9b65fb933a04p-10d, -0x1.bd7c08422410ap-10d,
                -0x1.ced7ad15228acp-10d, -0x1.ddb139971e14ap-10d, -0x1.e9f413f20401dp-10d, -0x1.f38f39a911e84p-10d, -0x1.fa755731c12f4p-10d,
                -0x1.fe9cda708fba8p-10d,
        };
        System.arraycopy(v, 0, t, 1024, v.length);
    }

    /**
     * Fills {@link #DCT} entries 1280 through 1535.
     *
     * @param t the table being assembled
     */
    private static void dct5(double[] t) {
        var v = new double[]{
                0x1.0p-9d, 0x1.fc2647088d584p-10d, 0x1.f0a7efa31b35ep-10d, 0x1.ddb13b3bd9ca2p-10d, 0x1.c38b2ec290904p-10d,
                0x1.a29a7981dd965p-10d, 0x1.7b5df17000131p-10d, 0x1.4e6caacdf38aap-10d, 0x1.1c73b26d592fcp-10d, 0x1.cc66e6ba39a62p-11d,
                0x1.58f9a4050312ep-11d, 0x1.c0b81f0da7e97p-12d, 0x1.917a5ad77a646p-13d, -0x1.9215a91f91208p-15d, -0x1.2c8110b8231a6p-12d,
                -0x1.111d2b49e237ep-11d, -0x1.87de2fa72f075p-11d, -0x1.f8ba52fca870ep-11d, -0x1.30ff825d851e8p-10d, -0x1.610b77c1a75bbp-10d,
                -0x1.8bc808f08339cp-10d, -0x1.b090a77ea3fefp-10d, -0x1.ced7b0ee38ce9p-10d, -0x1.e628900ba0736p-10d, -0x1.f6297dd3b8f3dp-10d,
                -0x1.fe9cdb2378718p-10d, -0x1.ff621dfdc0eb5p-10d, -0x1.f8764ed5d1c7cp-10d, -0x1.e9f413fbe930fp-10d, -0x1.d4134b001809dp-10d,
                -0x1.b7283196655d7p-10d, -0x1.93a2213886e96p-10d, -0x1.6a09e26627e0dp-10d, -0x1.3aff9df6a1605p-10d, -0x1.073874681dadfp-10d,
                -0x1.9ef788e568b54p-11d, -0x1.294056b9d64a9p-11d, -0x1.5e212a7604974p-12d, -0x1.91f5f386dcb2ep-14d, 0x1.2d524046d51e2p-13d,
                0x1.8f8b9f90b4445p-12d, 0x1.4135d70b3e6d8p-11d, 0x1.b5d10e108b16ap-11d, 0x1.11eb3bb0a399p-10d, 0x1.44cf38567f758p-10d,
                0x1.72d088fdb09aep-10d, 0x1.9b3e095979e55p-10d, 0x1.bd7c0ee132275p-10d, 0x1.d906c033e505fp-10d, 0x1.ed7410c6fcb4ap-10d,
                0x1.fa75593d22b4bp-10d, 0x1.ffd886410a49p-10d, 0x1.fd88d9e938977p-10d, 0x1.f38f3a00f0728p-10d, 0x1.e2120f10aa119p-10d,
                0x1.c954b0557a69cp-10d, 0x1.a9b66050e597p-10d, 0x1.83b0ddfd26a59p-10d, 0x1.57d6900567d38p-10d, 0x1.26d0510ef2ad3p-10d,
                0x1.e2b5cb1bd3707p-11d, 0x1.708849da03e1bp-11d, 0x1.f19f83dbbdafdp-12d, 0x1.f564bb6ae924p-13d, -0x1.5dde973dcb398p-32d,
                -0x1.f565123a29abbp-13d, -0x1.f19fae480a3bbp-12d, -0x1.70885e40d39fp-11d, -0x1.e2b5de64be522p-11d, -0x1.26d059ff54f0dp-10d,
                -0x1.57d6981f4b8a7p-10d, -0x1.83b0e5215a584p-10d, -0x1.a9b66663e9d18p-10d, -0x1.c954b53feb9d5p-10d, -0x1.e21212bf9aa35p-10d,
                -0x1.f38f3c66319d9p-10d, -0x1.fd88dafb9109p-10d, -0x1.ffd885d77dde7p-10d, -0x1.fa7556c61136cp-10d, -0x1.ed740c4be40d9p-10d,
                -0x1.d906b9c606146p-10d, -0x1.bd7c06994e4f5p-10d, -0x1.9b3dff5773be3p-10d, -0x1.72d07d6811296p-10d, -0x1.44cf2b59e1cafp-10d,
                -0x1.11eb2d7f09122p-10d, -0x1.b5d0efb0aa89fp-11d, -0x1.4135b723a73b5p-11d, -0x1.8f8b5da7cad8fp-12d, -0x1.2d51ba3d1dcbdp-13d,
                0x1.91f70001dbd8dp-14d, 0x1.5e216cac36b22p-12d, 0x1.294076e12f51dp-11d, 0x1.9ef7a79d33157p-11d, 0x1.073882d117dcdp-10d,
                0x1.3affab353439ep-10d, 0x1.6a09ee4754105p-10d, 0x1.93a22b8e8e767p-10d, 0x1.b7283a397bcd3p-10d, 0x1.d41351cefb9d7p-10d,
                0x1.e9f418dc62f37p-10d, 0x1.f87651b51a7e9p-10d, 0x1.ff621ed0c9648p-10d, 0x1.fe9cda559101p-10d, 0x1.f6297bb1ac53ap-10d,
                0x1.e6288c9da530ap-10d, 0x1.ced7ac41838f5p-10d, 0x1.b090a1a534a94p-10d, 0x1.8bc80200dfa4fp-10d, 0x1.610b6fd684972p-10d,
                0x1.30ff7995609bap-10d, 0x1.f8ba3ff5fcc81p-11d, 0x1.87de1b7363252p-11d, 0x1.111d1636c0009p-11d, 0x1.2c80e5757db32p-12d,
                0x1.92144b5bf3a2p-15d, -0x1.917ab1e34dbd3p-13d, -0x1.c0b849b971ep-12d, -0x1.58f9b89bb14aep-11d, -0x1.cc66fa426a764p-11d,
                -0x1.1c73bb849853ap-10d, -0x1.4e6cb3155866dp-10d, -0x1.7b5df8c7a9f94p-10d, -0x1.a29a7fcd86ebap-10d, -0x1.c38b33e9fba9bp-10d,
                -0x1.ddb13f2b2e435p-10d, -0x1.f0a7f24b325afp-10d, -0x1.fc26485f2c7ccp-10d, 0x1.0p-9d, 0x1.f8764f97da8fap-10d,
                0x1.e212101365523p-10d, 0x1.bd7c0a43450a2p-10d, 0x1.8bc805cf4d461p-10d, 0x1.4e6caa6d07523p-10d, 0x1.073877c7cf998p-10d,
                0x1.70884e843ef99p-11d, 0x1.8f8b78db3202cp-12d, 0x1.9214fb4e61c19p-15d, -0x1.2c8114ad0dc96p-12d, -0x1.4135d0860e9a1p-11d,
                -0x1.e2b5dadda7f8dp-11d, -0x1.3affa622d517bp-10d, -0x1.7b5df56c1196ap-10d, -0x1.b090a84c1464p-10d, -0x1.d906bf1484305p-10d,
                -0x1.f38f3c120f1b3p-10d, -0x1.ff621e8641668p-10d, -0x1.fc26463ef26cbp-10d, -0x1.e9f4136748f9dp-10d, -0x1.c954aeca4a1bdp-10d,
                -0x1.9b3dffeff3816p-10d, -0x1.610b6f86288bbp-10d, -0x1.1c73aca986bb5p-10d, -0x1.9ef78453386dp-11d, -0x1.f19f7498a030ap-12d,
                -0x1.2d51be3a5782dp-13d, 0x1.917ab951bf077p-13d, 0x1.111d399dee27bp-11d, 0x1.b5d1137d1218fp-11d, 0x1.26d05d9f411fcp-10d,
                0x1.6a09ee4754105p-10d, 0x1.a29a80a0d2bc4p-10d, 0x1.ced7b45291297p-10d, 0x1.ed7411b5f6247p-10d, 0x1.fd88db775b851p-10d,
                0x1.fe9cda4c266fp-10d, 0x1.f0a7edefaf841p-10d, 0x1.d41349f5d517ep-10d, 0x1.a9b65e17fe25cp-10d, 0x1.72d07db49360ap-10d,
                0x1.30ff78c7c196ep-10d, 0x1.cc66d9543e5c8p-11d, 0x1.294050db5b3a4p-11d, 0x1.f56497b038146p-13d, -0x1.91f701123c221p-14d,
                -0x1.c0b84f941f89fp-12d, -0x1.87de3e6f64dap-11d, -0x1.11eb3eb407058p-10d, -0x1.57d69bd3b658ap-10d, -0x1.93a22be7cac9p-10d,
                -0x1.c38b34db56899p-10d, -0x1.e62892b6329d7p-10d, -0x1.fa7559d5e35bep-10d, -0x1.ffd885b4efeb6p-10d, -0x1.f6297a5765595p-10d,
                -0x1.ddb136662fd67p-10d, -0x1.b7282cf5e7148p-10d, -0x1.83b0d72c8df37p-10d, -0x1.44cf26b689ap-10d, -0x1.f8ba330683279p-11d,
                -0x1.58f989c81e735p-11d, -0x1.5e2105053c892p-12d, 0x1.0666f16e5860fp-30d, 0x1.5e21864a389fap-12d, 0x1.58f9c78c293c7p-11d,
                0x1.f8ba6c1a85e97p-11d, 0x1.44cf4011676e6p-10d, 0x1.83b0ec9929053p-10d, 0x1.b7283dd2c149bp-10d, 0x1.ddb142342d684p-10d,
                0x1.f62980bd8af99p-10d, 0x1.ffd88683017fcp-10d, 0x1.fa7555e23016fp-10d, 0x1.e6288a4360317p-10d, 0x1.c38b282913fc7p-10d,
                0x1.93a21b55cc11fp-10d, 0x1.57d687dee2fb1p-10d, 0x1.11eb27f2cc8e8p-10d, 0x1.87de0cab2d188p-11d, 0x1.c0b7e6766e068p-12d,
                0x1.91f552a7d2c98p-14d, -0x1.f5656d890a301p-13d, -0x1.294084677a943p-11d, -0x1.cc670971ba2bfp-11d, -0x1.30ff8e69d6108p-10d,
                -0x1.72d09046ed379p-10d, -0x1.a9b66d0e9fc6ep-10d, -0x1.d41354dff3cbbp-10d, -0x1.f0a7f47b05a3dp-10d, -0x1.fe9cdc475f8c4p-10d,
                -0x1.fd88d84051ce7p-10d, -0x1.ed7408f6689fbp-10d, -0x1.ced7a64c713f3p-10d, -0x1.a29a6dbdd63cfp-10d, -0x1.6a09d715d8604p-10d,
                -0x1.26d042ce1a177p-10d, -0x1.b5d0d82fb64b6p-11d, -0x1.111cfa6487634p-11d, -0x1.9179b42e447c5p-13d, 0x1.2d52c3eb453e3p-13d,
                0x1.f19ff3dd8564bp-12d, 0x1.9ef7c04c37472p-11d, 0x1.1c73c7ef44027p-10d, 0x1.610b87479062ap-10d, 0x1.9b3e1379f532p-10d,
                0x1.c954bd899d48ap-10d, 0x1.e9f41cecc3cf8p-10d, 0x1.fc264a42cf911p-10d, 0x1.ff621d33ef3dep-10d, 0x1.f38f362b5d353p-10d,
                0x1.d906b4c5edf06p-10d, 0x1.b09099e34b0e8p-10d, 0x1.7b5de355b2aeap-10d, 0x1.3aff90e737a4fp-10d, 0x1.e2b5ab5c09acp-11d,
                0x1.41359d6025543p-11d, 0x1.2c80aa1bbd1ebp-12d, -0x1.921858ea84cd7p-15d, -0x1.8f8be28514335p-12d, -0x1.708880c62204dp-11d,
                -0x1.07388ee1cf8bdp-10d, -0x1.4e6cbed1f2dc6p-10d, -0x1.8bc816e569e79p-10d, -0x1.bd7c1789c75f1p-10d, -0x1.e21219263be89p-10d,
                -0x1.f8765432a033fp-10d,
        };
        System.arraycopy(v, 0, t, 1280, v.length);
    }

    /**
     * Fills {@link #DCT} entries 1536 through 1791.
     *
     * @param t the table being assembled
     */
    private static void dct6(double[] t) {
        var v = new double[]{
                0x1.0p-9d, 0x1.ff621e370373ep-10d, 0x1.fd88da3ac5782p-10d, 0x1.fa7557eb600f8p-10d, 0x1.f6297cf64db9ap-10d,
                0x1.f0a7efaae1b44p-10d, 0x1.e9f41557f314ep-10d, 0x1.e2121033bcb9fp-10d, 0x1.d906bccf3cb87p-10d, 0x1.ced7af16a5c38p-10d,
                0x1.c38b2ee0bbed9p-10d, 0x1.b728340f3bep-10d, 0x1.a9b66242b014ap-10d, 0x1.9b3e04245a916p-10d, 0x1.8bc806491af94p-10d,
                0x1.7b5df1b0784edp-10d, 0x1.6a09e5e333598p-10d, 0x1.57d692b5021d8p-10d, 0x1.44cf31ad4e182p-10d, 0x1.30ff7f1b05d3ep-10d,
                0x1.1c73b2d7c6c89p-10d, 0x1.073878bed6376p-10d, 0x1.e2b5d1b91d662p-11d, 0x1.b5d0feb62843bp-11d, 0x1.87de2862925c2p-11d,
                0x1.58f9a5324e5d6p-11d, 0x1.294060a578f7bp-11d, 0x1.f19f92e4d337ap-12d, 0x1.8f8b7ebdae7afp-12d, 0x1.2c8101a685e28p-12d,
                0x1.917a60d014f3ap-13d, 0x1.91f6485bf7853p-14d, -0x1.777a5cf72ceccp-34d, -0x1.91f6773cca10dp-14d, -0x1.917a782acd13p-13d,
                -0x1.2c810d41d5c1p-12d, -0x1.8f8b8a3fc9abbp-12d, -0x1.f19f9e46a0c98p-12d, -0x1.29406642b66ecp-11d, -0x1.58f9aab86c203p-11d,
                -0x1.87de2dce284b4p-11d, -0x1.b5d10403de9c6p-11d, -0x1.e2b5d6e5aed21p-11d, -0x1.07387b42f4044p-10d, -0x1.1c73b5482c6ep-10d,
                -0x1.30ff8176323cdp-10d, -0x1.44cf33f1cd466p-10d, -0x1.57d694e16e0f2p-10d, -0x1.6a09e7f634e57p-10d, -0x1.7b5df3a8c7f8dp-10d,
                -0x1.8bc8082581bbbp-10d, -0x1.9b3e05e3b29d2p-10d, -0x1.a9b663e3e5861p-10d, -0x1.b72835914d696p-10d, -0x1.c38b3042bb75ap-10d,
                -0x1.ced7b057b8f86p-10d, -0x1.d906bdee9d962p-10d, -0x1.e2121130ba049p-10d, -0x1.e9f41631f0c5fp-10d, -0x1.f0a7f061595bp-10d,
                -0x1.f6297d88cecdbp-10d, -0x1.fa75585990368p-10d, -0x1.fd88da8460bd6p-10d, -0x1.ff621e5bdc723p-10d, -0x1.fffffffffffdep-10d,
                -0x1.ff621e122a714p-10d, -0x1.fd88d9f12a2eap-10d, -0x1.fa75577d2fe44p-10d, -0x1.f6297c63cca16p-10d, -0x1.f0a7eef46a096p-10d,
                -0x1.e9f4147df55fap-10d, -0x1.e2120f36bf6b4p-10d, -0x1.d906bbafdbd6dp-10d, -0x1.ced7add5928acp-10d, -0x1.c38b2d7ebc61bp-10d,
                -0x1.b728328d2a52fp-10d, -0x1.a9b660a17a9fap-10d, -0x1.9b3e026502824p-10d, -0x1.8bc8046cb4337p-10d, -0x1.7b5defb828a1ap-10d,
                -0x1.6a09e3d031ca8p-10d, -0x1.57d6908896291p-10d, -0x1.44cf2f68cee72p-10d, -0x1.30ff7cbfd9686p-10d, -0x1.1c73b0676120cp-10d,
                -0x1.0738763ab8684p-10d, -0x1.e2b5cc8c8bf62p-11d, -0x1.b5d0f96871e76p-11d, -0x1.87de22f6fc69bp-11d, -0x1.58f99fac3097bp-11d,
                -0x1.29405b083b7e3p-11d, -0x1.f19f878305a19p-12d, -0x1.8f8b733b9346ep-12d, -0x1.2c80f60b36017p-12d, -0x1.917a49755cd0dp-13d,
                -0x1.91f6197b24f63p-14d, 0x1.199bc5b961b0dp-32d, 0x1.91f6a61d9c991p-14d, 0x1.917a8f85852f1p-13d, 0x1.2c8118dd259dp-12d,
                0x1.8f8b95c1e4d9p-12d, 0x1.f19fa9a86e573p-12d, 0x1.29406bdff3e34p-11d, 0x1.58f9b03e89e01p-11d, 0x1.87de3339be371p-11d,
                0x1.b5d1095194f17p-11d, 0x1.e2b5dc12403ap-11d, 0x1.07387dc711cefp-10d, 0x1.1c73b7b892111p-10d, 0x1.30ff83d15ea34p-10d,
                0x1.44cf36364c71ep-10d, 0x1.57d6970dd9fdcp-10d, 0x1.6a09ea09366e6p-10d, 0x1.7b5df5a1179fap-10d, 0x1.8bc80a01e87adp-10d,
                0x1.9b3e07a30aa56p-10d, 0x1.a9b665851af3ep-10d, 0x1.b72837135eefp-10d, 0x1.c38b31a4baf9ep-10d, 0x1.ced7b198cc295p-10d,
                0x1.d906bf0dfe6fdp-10d, 0x1.e212122db74b2p-10d, 0x1.e9f4170bee72fp-10d, 0x1.f0a7f117d0fd9p-10d, 0x1.f6297e1b4fdd8p-10d,
                0x1.fa7558c7c0594p-10d, 0x1.fd88dacdfbfe5p-10d, 0x1.ff621e80b56c4p-10d, 0x1.0p-9d, 0x1.fd88da3ac5782p-10d,
                0x1.f6297cf64db9ap-10d, 0x1.e9f41557f314ep-10d, 0x1.d906bccf3cb87p-10d, 0x1.c38b2ee0bbed9p-10d, 0x1.a9b66242b014ap-10d,
                0x1.8bc806491af94p-10d, 0x1.6a09e5e333598p-10d, 0x1.44cf31ad4e182p-10d, 0x1.1c73b2d7c6c89p-10d, 0x1.e2b5d1b91d662p-11d,
                0x1.87de2862925c2p-11d, 0x1.294060a578f7bp-11d, 0x1.8f8b7ebdae7afp-12d, 0x1.917a60d014f3ap-13d, -0x1.777a5cf72ceccp-34d,
                -0x1.917a782acd13p-13d, -0x1.8f8b8a3fc9abbp-12d, -0x1.29406642b66ecp-11d, -0x1.87de2dce284b4p-11d, -0x1.e2b5d6e5aed21p-11d,
                -0x1.1c73b5482c6ep-10d, -0x1.44cf33f1cd466p-10d, -0x1.6a09e7f634e57p-10d, -0x1.8bc8082581bbbp-10d, -0x1.a9b663e3e5861p-10d,
                -0x1.c38b3042bb75ap-10d, -0x1.d906bdee9d962p-10d, -0x1.e9f41631f0c5fp-10d, -0x1.f6297d88cecdbp-10d, -0x1.fd88da8460bd6p-10d,
                -0x1.fffffffffffdep-10d, -0x1.fd88d9f12a2eap-10d, -0x1.f6297c63cca16p-10d, -0x1.e9f4147df55fap-10d, -0x1.d906bbafdbd6dp-10d,
                -0x1.c38b2d7ebc61bp-10d, -0x1.a9b660a17a9fap-10d, -0x1.8bc8046cb4337p-10d, -0x1.6a09e3d031ca8p-10d, -0x1.44cf2f68cee72p-10d,
                -0x1.1c73b0676120cp-10d, -0x1.e2b5cc8c8bf62p-11d, -0x1.87de22f6fc69bp-11d, -0x1.29405b083b7e3p-11d, -0x1.8f8b733b9346ep-12d,
                -0x1.917a49755cd0dp-13d, 0x1.199bc5b961b0dp-32d, 0x1.917a8f85852f1p-13d, 0x1.8f8b95c1e4d9p-12d, 0x1.29406bdff3e34p-11d,
                0x1.87de3339be371p-11d, 0x1.e2b5dc12403ap-11d, 0x1.1c73b7b892111p-10d, 0x1.44cf36364c71ep-10d, 0x1.6a09ea09366e6p-10d,
                0x1.8bc80a01e87adp-10d, 0x1.a9b665851af3ep-10d, 0x1.c38b31a4baf9ep-10d, 0x1.d906bf0dfe6fdp-10d, 0x1.e9f4170bee72fp-10d,
                0x1.f6297e1b4fdd8p-10d, 0x1.fd88dacdfbfe5p-10d, 0x1.0p-9d, 0x1.fd88da3ac5782p-10d, 0x1.f6297cf64db9ap-10d,
                0x1.e9f41557f314ep-10d, 0x1.d906bccf3cb87p-10d, 0x1.c38b2ee0bbed9p-10d, 0x1.a9b66242b014ap-10d, 0x1.8bc806491af94p-10d,
                0x1.6a09e5e333598p-10d, 0x1.44cf31ad4e182p-10d, 0x1.1c73b2d7c6c89p-10d, 0x1.e2b5d1b91d662p-11d, 0x1.87de2862925c2p-11d,
                0x1.294060a578f7bp-11d, 0x1.8f8b7ebdae7afp-12d, 0x1.917a60d014f3ap-13d, -0x1.777a5cf72ceccp-34d, -0x1.917a782acd13p-13d,
                -0x1.8f8b8a3fc9abbp-12d, -0x1.29406642b66ecp-11d, -0x1.87de2dce284b4p-11d, -0x1.e2b5d6e5aed21p-11d, -0x1.1c73b5482c6ep-10d,
                -0x1.44cf33f1cd466p-10d, -0x1.6a09e7f634e57p-10d, -0x1.8bc8082581bbbp-10d, -0x1.a9b663e3e5861p-10d, -0x1.c38b3042bb75ap-10d,
                -0x1.d906bdee9d962p-10d, -0x1.e9f41631f0c5fp-10d, -0x1.f6297d88cecdbp-10d, -0x1.fd88da8460bd6p-10d, -0x1.fffffffffffdep-10d,
                -0x1.fd88d9f12a2eap-10d, -0x1.f6297c63cca16p-10d, -0x1.e9f4147df55fap-10d, -0x1.d906bbafdbd6dp-10d, -0x1.c38b2d7ebc61bp-10d,
                -0x1.a9b660a17a9fap-10d, -0x1.8bc8046cb4337p-10d, -0x1.6a09e3d031ca8p-10d, -0x1.44cf2f68cee72p-10d, -0x1.1c73b0676120cp-10d,
                -0x1.e2b5cc8c8bf62p-11d, -0x1.87de22f6fc69bp-11d, -0x1.29405b083b7e3p-11d, -0x1.8f8b733b9346ep-12d, -0x1.917a49755cd0dp-13d,
                0x1.199bc5b961b0dp-32d, 0x1.917a8f85852f1p-13d, 0x1.8f8b95c1e4d9p-12d, 0x1.29406bdff3e34p-11d, 0x1.87de3339be371p-11d,
                0x1.e2b5dc12403ap-11d, 0x1.1c73b7b892111p-10d, 0x1.44cf36364c71ep-10d, 0x1.6a09ea09366e6p-10d, 0x1.8bc80a01e87adp-10d,
                0x1.a9b665851af3ep-10d, 0x1.c38b31a4baf9ep-10d, 0x1.d906bf0dfe6fdp-10d, 0x1.e9f4170bee72fp-10d, 0x1.f6297e1b4fdd8p-10d,
                0x1.fd88dacdfbfe5p-10d,
        };
        System.arraycopy(v, 0, t, 1536, v.length);
    }

    /**
     * Fills {@link #DCT} entries 1792 through 2047.
     *
     * @param t the table being assembled
     */
    private static void dct7(double[] t) {
        var v = new double[]{
                0x1.0p-9d, 0x1.fa7557f01213ap-10d, 0x1.e9f4156a871b1p-10d, 0x1.ced7af3fb15bap-10d, 0x1.a9b66289cd01ap-10d,
                0x1.7b5df21beb5cep-10d, 0x1.44cf3241b91abp-10d, 0x1.0738797ef7ce6p-10d, 0x1.87de2a3b99195p-11d, 0x1.f19f97424d136p-12d,
                0x1.917a6ac3c1373p-13d, -0x1.91f6614392c4ep-14d, -0x1.8f8b845d4d353p-12d, -0x1.58f9a7a90ec63p-11d, -0x1.e2b5d3cf7b408p-11d,
                -0x1.30ff7ff4a8194p-10d, -0x1.6a09e68c2affep-10d, -0x1.9b3e049fa3266p-10d, -0x1.c38b2f33353p-10d, -0x1.e2121063e5cadp-10d,
                -0x1.f6297d0bf3366p-10d, -0x1.ff621e3ae33e7p-10d, -0x1.fd88da362b3b6p-10d, -0x1.f0a7efa73f665p-10d, -0x1.d906bcd5c2792p-10d,
                -0x1.b728342872936p-10d, -0x1.8bc8067c84e7ap-10d, -0x1.57d69308c4b4p-10d, -0x1.1c73b350604f4p-10d, -0x1.b5d0fff63fc61p-11d,
                -0x1.294062358ed0fp-11d, -0x1.2c810560436dap-12d, 0x1.99bc5b961b1adp-36d, 0x1.2c81088add8a8p-12d, 0x1.294063bda68b5p-11d,
                0x1.b5d10168a5579p-11d, 0x1.1c73b3fab7b6bp-10d, 0x1.57d693a090c22p-10d, 0x1.8bc806fe7c689p-10d, 0x1.b7283491c54cdp-10d,
                0x1.d906bd2428c1cp-10d, 0x1.f0a7efd906c8p-10d, 0x1.fd88da4a3fdb3p-10d, 0x1.ff621e7a87cecp-10d, 0x1.f6297e08fd9d5p-10d,
                0x1.e2121218dbc6bp-10d, 0x1.c38b3196a1477p-10d, 0x1.9b3e07a449135p-10d, 0x1.6a09ea215107fp-10d, 0x1.30ff840673bdap-10d,
                0x1.e2b5dcbf43464p-11d, 0x1.58f9b1338180fp-11d, 0x1.8f8b983dc9cefp-12d, 0x1.91f6b23b47c1cp-14d, -0x1.917a426d5e09cp-13d,
                -0x1.f19f83999ba38p-12d, -0x1.87de20def91c2p-11d, -0x1.0738752674c7p-10d, -0x1.44cf2e57182aap-10d, -0x1.7b5deeb4e0b5cp-10d,
                -0x1.a9b65fb933a04p-10d, -0x1.ced7ad15228acp-10d, -0x1.e9f413f20401dp-10d, -0x1.fa755731c12f4p-10d, -0x1.fffffffffff99p-10d,
                -0x1.fa7558ae62eb4p-10d, -0x1.e9f416e30a281p-10d, -0x1.ced7b16a4020ep-10d, -0x1.a9b6655a66584p-10d, -0x1.7b5df582f5fa9p-10d,
                -0x1.44cf362c5a02bp-10d, -0x1.07387dd77acf3p-10d, -0x1.87de3398390cap-11d, -0x1.f19faaeafe76dp-12d, -0x1.917a931a245a8p-13d,
                0x1.91f6104bddbdep-14d, 0x1.8f8b707cd0917p-12d, 0x1.58f99e1e9c02dp-11d, 0x1.e2b5cadfb32ecp-11d, 0x1.30ff7be2dc6d3p-10d,
                0x1.6a09e2f704eedp-10d, 0x1.9b3e019afd2f3p-10d, 0x1.c38b2ccfc90d3p-10d, 0x1.e2120eaeefc2dp-10d, 0x1.f6297c0ee8c2dp-10d,
                0x1.ff621dfb3ea16p-10d, 0x1.fd88db48839c4p-10d, 0x1.f0a7f24f568bp-10d, 0x1.d906c104df93p-10d, 0x1.b72839c765ec1p-10d,
                0x1.8bc80d6c285e9p-10d, 0x1.57d69b22a85cp-10d, 0x1.1c73bc679f6fep-10d, 0x1.b5d113bab3818p-11d, 0x1.294077226ce37p-11d,
                0x1.2c8130a2e8bbp-12d, 0x1.5110b4611a5c5p-31d, -0x1.2c80dd48381cap-12d, -0x1.29404ed0c858bp-11d, -0x1.b5d0eda4316cbp-11d,
                -0x1.1c73aae378774p-10d, -0x1.57d68b86acf4ep-10d, -0x1.8bc8000ed8c6dp-10d, -0x1.b7282ef2d1c4ap-10d, -0x1.d906b8f50b74ap-10d,
                -0x1.f0a7ed30ef6d9p-10d, -0x1.fd88d937e7433p-10d, -0x1.ff621eba2c523p-10d, -0x1.f6297f0607f7bp-10d, -0x1.e21213cdd1b68p-10d,
                -0x1.c38b33fa0d539p-10d, -0x1.9b3e0aa8eef5ep-10d, -0x1.6a09edb67706ep-10d, -0x1.30ff88183f5a6p-10d, -0x1.e2b5e5af0b3fdp-11d,
                -0x1.58f9babdf4331p-11d, -0x1.8f8bac1e465eap-12d, -0x1.91f70332fcb4ap-14d, 0x1.917a1a16fad25p-13d, 0x1.f19f6ff0ea272p-12d,
                0x1.87de178259152p-11d, 0x1.073870cdf1b8fp-10d, 0x1.44cf2a6c77326p-10d, 0x1.7b5deb4dd6052p-10d, 0x1.a9b65ce89a344p-10d,
                0x1.ced7aaea93ae4p-10d, 0x1.e9f4127980dc3p-10d, 0x1.fa755673703e3p-10d, 0x1.0p-9d, 0x1.f6297cf64db9ap-10d,
                0x1.d906bccf3cb87p-10d, 0x1.a9b66242b014ap-10d, 0x1.6a09e5e333598p-10d, 0x1.1c73b2d7c6c89p-10d, 0x1.87de2862925c2p-11d,
                0x1.8f8b7ebdae7afp-12d, -0x1.777a5cf72ceccp-34d, -0x1.8f8b8a3fc9abbp-12d, -0x1.87de2dce284b4p-11d, -0x1.1c73b5482c6ep-10d,
                -0x1.6a09e7f634e57p-10d, -0x1.a9b663e3e5861p-10d, -0x1.d906bdee9d962p-10d, -0x1.f6297d88cecdbp-10d, -0x1.fffffffffffdep-10d,
                -0x1.f6297c63cca16p-10d, -0x1.d906bbafdbd6dp-10d, -0x1.a9b660a17a9fap-10d, -0x1.6a09e3d031ca8p-10d, -0x1.1c73b0676120cp-10d,
                -0x1.87de22f6fc69bp-11d, -0x1.8f8b733b9346ep-12d, 0x1.199bc5b961b0dp-32d, 0x1.8f8b95c1e4d9p-12d, 0x1.87de3339be371p-11d,
                0x1.1c73b7b892111p-10d, 0x1.6a09ea09366e6p-10d, 0x1.a9b665851af3ep-10d, 0x1.d906bf0dfe6fdp-10d, 0x1.f6297e1b4fdd8p-10d,
                0x1.0p-9d, 0x1.f6297cf64db9ap-10d, 0x1.d906bccf3cb87p-10d, 0x1.a9b66242b014ap-10d, 0x1.6a09e5e333598p-10d,
                0x1.1c73b2d7c6c89p-10d, 0x1.87de2862925c2p-11d, 0x1.8f8b7ebdae7afp-12d, -0x1.777a5cf72ceccp-34d, -0x1.8f8b8a3fc9abbp-12d,
                -0x1.87de2dce284b4p-11d, -0x1.1c73b5482c6ep-10d, -0x1.6a09e7f634e57p-10d, -0x1.a9b663e3e5861p-10d, -0x1.d906bdee9d962p-10d,
                -0x1.f6297d88cecdbp-10d, -0x1.fffffffffffdep-10d, -0x1.f6297c63cca16p-10d, -0x1.d906bbafdbd6dp-10d, -0x1.a9b660a17a9fap-10d,
                -0x1.6a09e3d031ca8p-10d, -0x1.1c73b0676120cp-10d, -0x1.87de22f6fc69bp-11d, -0x1.8f8b733b9346ep-12d, 0x1.199bc5b961b0dp-32d,
                0x1.8f8b95c1e4d9p-12d, 0x1.87de3339be371p-11d, 0x1.1c73b7b892111p-10d, 0x1.6a09ea09366e6p-10d, 0x1.a9b665851af3ep-10d,
                0x1.d906bf0dfe6fdp-10d, 0x1.f6297e1b4fdd8p-10d, 0x1.0p-9d, 0x1.f6297cf64db9ap-10d, 0x1.d906bccf3cb87p-10d,
                0x1.a9b66242b014ap-10d, 0x1.6a09e5e333598p-10d, 0x1.1c73b2d7c6c89p-10d, 0x1.87de2862925c2p-11d, 0x1.8f8b7ebdae7afp-12d,
                -0x1.777a5cf72ceccp-34d, -0x1.8f8b8a3fc9abbp-12d, -0x1.87de2dce284b4p-11d, -0x1.1c73b5482c6ep-10d, -0x1.6a09e7f634e57p-10d,
                -0x1.a9b663e3e5861p-10d, -0x1.d906bdee9d962p-10d, -0x1.f6297d88cecdbp-10d, -0x1.fffffffffffdep-10d, -0x1.f6297c63cca16p-10d,
                -0x1.d906bbafdbd6dp-10d, -0x1.a9b660a17a9fap-10d, -0x1.6a09e3d031ca8p-10d, -0x1.1c73b0676120cp-10d, -0x1.87de22f6fc69bp-11d,
                -0x1.8f8b733b9346ep-12d, 0x1.199bc5b961b0dp-32d, 0x1.8f8b95c1e4d9p-12d, 0x1.87de3339be371p-11d, 0x1.1c73b7b892111p-10d,
                0x1.6a09ea09366e6p-10d, 0x1.a9b665851af3ep-10d, 0x1.d906bf0dfe6fdp-10d, 0x1.f6297e1b4fdd8p-10d, 0x1.0p-9d,
                0x1.f6297cf64db9ap-10d, 0x1.d906bccf3cb87p-10d, 0x1.a9b66242b014ap-10d, 0x1.6a09e5e333598p-10d, 0x1.1c73b2d7c6c89p-10d,
                0x1.87de2862925c2p-11d, 0x1.8f8b7ebdae7afp-12d, -0x1.777a5cf72ceccp-34d, -0x1.8f8b8a3fc9abbp-12d, -0x1.87de2dce284b4p-11d,
                -0x1.1c73b5482c6ep-10d, -0x1.6a09e7f634e57p-10d, -0x1.a9b663e3e5861p-10d, -0x1.d906bdee9d962p-10d, -0x1.f6297d88cecdbp-10d,
                -0x1.fffffffffffdep-10d, -0x1.f6297c63cca16p-10d, -0x1.d906bbafdbd6dp-10d, -0x1.a9b660a17a9fap-10d, -0x1.6a09e3d031ca8p-10d,
                -0x1.1c73b0676120cp-10d, -0x1.87de22f6fc69bp-11d, -0x1.8f8b733b9346ep-12d, 0x1.199bc5b961b0dp-32d, 0x1.8f8b95c1e4d9p-12d,
                0x1.87de3339be371p-11d, 0x1.1c73b7b892111p-10d, 0x1.6a09ea09366e6p-10d, 0x1.a9b665851af3ep-10d, 0x1.d906bf0dfe6fdp-10d,
                0x1.f6297e1b4fdd8p-10d,
        };
        System.arraycopy(v, 0, t, 1792, v.length);
    }

    /**
     * Leading 20 ms sine taper analysis window, {@code 264} entries.
     */
    static final float[] WIN1_20MS = {
            0x1.8476ecp-8f, 0x1.84752ep-7f, 0x1.2355b4p-6f, 0x1.846e3p-6f, 0x1.e58332p-6f, 0x1.2349e8p-5f, 0x1.53cf9ap-5f, 
            0x1.84523ep-5f, 0x1.b4d164p-5f, 0x1.e54c9cp-5f, 0x1.0ae1bap-4f, 0x1.231ac2p-4f, 0x1.3b5128p-4f, 0x1.5384bap-4f, 
            0x1.6bb54p-4f, 0x1.83e27ep-4f, 0x1.9c0c4p-4f, 0x1.b4324ep-4f, 0x1.cc546ep-4f, 0x1.e4726ap-4f, 0x1.fc8c0cp-4f, 
            0x1.0a508cp-3f, 0x1.1658aep-3f, 0x1.225e4ep-3f, 0x1.2e6154p-3f, 0x1.3a619ep-3f, 0x1.465f16p-3f, 0x1.5259ap-3f, 
            0x1.5e511cp-3f, 0x1.6a4574p-3f, 0x1.763688p-3f, 0x1.82243ep-3f, 0x1.8e0e7cp-3f, 0x1.99f526p-3f, 0x1.a5d82p-3f, 
            0x1.b1b74ep-3f, 0x1.bd9296p-3f, 0x1.c969dap-3f, 0x1.d53d02p-3f, 0x1.e10bf2p-3f, 0x1.ecd68cp-3f, 0x1.f89cbap-3f, 
            0x1.022f2ep-2f, 0x1.080daep-2f, 0x1.0de9ccp-2f, 0x1.13c37ep-2f, 0x1.199ab4p-2f, 0x1.1f6f62p-2f, 0x1.25417ap-2f, 
            0x1.2b10eep-2f, 0x1.30ddb2p-2f, 0x1.36a7bap-2f, 0x1.3c6ef4p-2f, 0x1.423356p-2f, 0x1.47f4d2p-2f, 0x1.4db35cp-2f, 
            0x1.536ee4p-2f, 0x1.59276p-2f, 0x1.5edccp-2f, 0x1.648ef8p-2f, 0x1.6a3dfcp-2f, 0x1.6fe9bep-2f, 0x1.75923p-2f, 
            0x1.7b3746p-2f, 0x1.80d8f2p-2f, 0x1.867728p-2f, 0x1.8c11dcp-2f, 0x1.91a9p-2f, 0x1.973c86p-2f, 0x1.9ccc66p-2f, 
            0x1.a2588ap-2f, 0x1.a7e0eep-2f, 0x1.ad658p-2f, 0x1.b2e638p-2f, 0x1.b86304p-2f, 0x1.bddbdcp-2f, 0x1.c350aep-2f, 
            0x1.c8c174p-2f, 0x1.ce2e1cp-2f, 0x1.d396ap-2f, 0x1.d8faeap-2f, 0x1.de5af6p-2f, 0x1.e3b6b2p-2f, 0x1.e90e18p-2f, 
            0x1.ee6114p-2f, 0x1.f3afa2p-2f, 0x1.f8f9acp-2f, 0x1.fe3f3p-2f, 0x1.01c00cp-1f, 0x1.045e32p-1f, 0x1.06f9fep-1f, 
            0x1.09936cp-1f, 0x1.0c2a78p-1f, 0x1.0ebf1ap-1f, 0x1.11514cp-1f, 0x1.13e10ap-1f, 0x1.166e4cp-1f, 0x1.18f90cp-1f, 
            0x1.1b8146p-1f, 0x1.1e06f4p-1f, 0x1.208a0ep-1f, 0x1.230a9p-1f, 0x1.258872p-1f, 0x1.2803b4p-1f, 0x1.2a7c48p-1f, 
            0x1.2cf23p-1f, 0x1.2f6562p-1f, 0x1.31d5dap-1f, 0x1.34439p-1f, 0x1.36ae82p-1f, 0x1.3916a8p-1f, 0x1.3b7bfep-1f, 
            0x1.3dde7cp-1f, 0x1.403e1ep-1f, 0x1.429aep-1f, 0x1.44f4bap-1f, 0x1.474ba8p-1f, 0x1.499fa6p-1f, 0x1.4bf0aap-1f, 
            0x1.4e3eb4p-1f, 0x1.5089bap-1f, 0x1.52d1bcp-1f, 0x1.5516bp-1f, 0x1.575894p-1f, 0x1.59975ep-1f, 0x1.5bd31p-1f, 
            0x1.5e0bap-1f, 0x1.60410ap-1f, 0x1.627348p-1f, 0x1.64a256p-1f, 0x1.66ce3p-1f, 0x1.68f6cep-1f, 0x1.6b1c2ep-1f, 
            0x1.6d3e4ap-1f, 0x1.6f5d1ep-1f, 0x1.7178a2p-1f, 0x1.7390d4p-1f, 0x1.75a5aep-1f, 0x1.77b72cp-1f, 0x1.79c54ap-1f, 
            0x1.7bdp-1f, 0x1.7dd74cp-1f, 0x1.7fdb2ap-1f, 0x1.81db94p-1f, 0x1.83d884p-1f, 0x1.85d1f8p-1f, 0x1.87c7eap-1f, 
            0x1.89ba56p-1f, 0x1.8ba938p-1f, 0x1.8d948ap-1f, 0x1.8f7c4ap-1f, 0x1.91607p-1f, 0x1.9340fap-1f, 0x1.951de4p-1f, 
            0x1.96f72cp-1f, 0x1.98ccc6p-1f, 0x1.9a9eb6p-1f, 0x1.9c6cf2p-1f, 0x1.9e377cp-1f, 0x1.9ffe48p-1f, 0x1.a1c158p-1f, 
            0x1.a380a6p-1f, 0x1.a53c3p-1f, 0x1.a6f3eep-1f, 0x1.a8a7dep-1f, 0x1.aa57fcp-1f, 0x1.ac0446p-1f, 0x1.adacb4p-1f, 
            0x1.af5144p-1f, 0x1.b0f1f6p-1f, 0x1.b28ec2p-1f, 0x1.b427a6p-1f, 0x1.b5bc9ap-1f, 0x1.b74da2p-1f, 0x1.b8dab6p-1f, 
            0x1.ba63d2p-1f, 0x1.bbe8f4p-1f, 0x1.bd6a16p-1f, 0x1.bee738p-1f, 0x1.c06056p-1f, 0x1.c1d56ap-1f, 0x1.c3467p-1f, 
            0x1.c4b36ap-1f, 0x1.c61c52p-1f, 0x1.c78124p-1f, 0x1.c8e1dap-1f, 0x1.ca3e78p-1f, 0x1.cb96f4p-1f, 0x1.cceb5p-1f, 
            0x1.ce3b84p-1f, 0x1.cf8792p-1f, 0x1.d0cf74p-1f, 0x1.d21328p-1f, 0x1.d352a8p-1f, 0x1.d48dfap-1f, 0x1.d5c51p-1f, 
            0x1.d6f7eep-1f, 0x1.d8268ep-1f, 0x1.d950f2p-1f, 0x1.da771p-1f, 0x1.db98ecp-1f, 0x1.dcb68p-1f, 0x1.ddcfccp-1f, 
            0x1.dee4c8p-1f, 0x1.dff57ap-1f, 0x1.e101d6p-1f, 0x1.e209e4p-1f, 0x1.e30d98p-1f, 0x1.e40cf4p-1f, 0x1.e507f6p-1f, 
            0x1.e5fe9cp-1f, 0x1.e6f0e2p-1f, 0x1.e7dec6p-1f, 0x1.e8c848p-1f, 0x1.e9ad64p-1f, 0x1.ea8e18p-1f, 0x1.eb6a64p-1f, 
            0x1.ec4242p-1f, 0x1.ed15b6p-1f, 0x1.ede4b8p-1f, 0x1.eeaf48p-1f, 0x1.ef7566p-1f, 0x1.f0371p-1f, 0x1.f0f442p-1f, 
            0x1.f1acfep-1f, 0x1.f2613cp-1f, 0x1.f31102p-1f, 0x1.f3bc4ap-1f, 0x1.f46312p-1f, 0x1.f5055cp-1f, 0x1.f5a324p-1f, 
            0x1.f63c66p-1f, 0x1.f6d126p-1f, 0x1.f7616p-1f, 0x1.f7ed12p-1f, 0x1.f8743cp-1f, 0x1.f8f6dep-1f, 0x1.f974f2p-1f, 
            0x1.f9ee7ep-1f, 0x1.fa637ap-1f, 0x1.fad3e8p-1f, 0x1.fb3fc8p-1f, 0x1.fba71ap-1f, 0x1.fc09d8p-1f, 0x1.fc6804p-1f, 
            0x1.fcc19ep-1f, 0x1.fd16a6p-1f, 0x1.fd6718p-1f, 0x1.fdb2f6p-1f, 0x1.fdfa3ep-1f, 0x1.fe3cfp-1f, 0x1.fe7b0cp-1f, 
            0x1.feb49p-1f, 0x1.fee97ap-1f, 0x1.ff19cep-1f, 0x1.ff4588p-1f, 0x1.ff6ca8p-1f, 0x1.ff8f3p-1f, 0x1.ffad1ep-1f, 
            0x1.ffc67p-1f, 0x1.ffdb28p-1f, 0x1.ffeb46p-1f, 0x1.fff6cap-1f, 0x1.fffdb2p-1f, 
    };

    /**
     * Long trailing cosine taper analysis window, {@code 64} entries.
     */
    static final float[] WIN3_LONG = {
            0x1.ffd9bap-1f, 0x1.ff66fp-1f, 0x1.fea7b2p-1f, 0x1.fd9c1cp-1f, 0x1.fc4456p-1f, 0x1.faa096p-1f, 0x1.f8b116p-1f, 
            0x1.f67626p-1f, 0x1.f3f016p-1f, 0x1.f11f4ap-1f, 0x1.ee042ap-1f, 0x1.ea9f32p-1f, 0x1.e6f0e2p-1f, 0x1.e2f9c4p-1f, 
            0x1.deba72p-1f, 0x1.da339p-1f, 0x1.d565c8p-1f, 0x1.d051d6p-1f, 0x1.caf878p-1f, 0x1.c55a7ep-1f, 0x1.bf78bcp-1f, 
            0x1.b95416p-1f, 0x1.b2ed76p-1f, 0x1.ac45dp-1f, 0x1.a55e24p-1f, 0x1.9e377ap-1f, 0x1.96d2e2p-1f, 0x1.8f317ap-1f, 
            0x1.875462p-1f, 0x1.7f3ccep-1f, 0x1.76ebecp-1f, 0x1.6e62fcp-1f, 0x1.65a346p-1f, 0x1.5cae1ap-1f, 0x1.5384dp-1f, 
            0x1.4a28cp-1f, 0x1.409b56p-1f, 0x1.36dep-1f, 0x1.2cf22ep-1f, 0x1.22d96p-1f, 0x1.189518p-1f, 0x1.0e26dcp-1f, 
            0x1.03903cp-1f, 0x1.f1a59ep-2f, 0x1.dbe06p-2f, 0x1.c5d3fcp-2f, 0x1.af83cp-2f, 0x1.98f3p-2f, 0x1.822526p-2f, 
            0x1.6b1d8ap-2f, 0x1.53dfa6p-2f, 0x1.3c6ef2p-2f, 0x1.24ceeep-2f, 0x1.0d0326p-2f, 0x1.ea1e4cp-3f, 0x1.b9ed06p-3f, 
            0x1.8979bp-3f, 0x1.58cb88p-3f, 0x1.27e9d2p-3f, 0x1.edb7ep-4f, 0x1.8b522ep-4f, 0x1.28b16p-4f, 0x1.8bc874p-5f, 
            0x1.8be5f4p-6f, 
    };

    /**
     * Short trailing cosine taper analysis window, {@code 32} entries.
     */
    static final float[] WIN3_SHORT = {
            0x1.ff6b8ap-1f, 0x1.fdae7ep-1f, 0x1.fac9ep-1f, 0x1.f6bf5cp-1f, 0x1.f1914ap-1f, 0x1.eb42aap-1f, 0x1.e3d726p-1f, 
            0x1.db530ap-1f, 0x1.d1bb48p-1f, 0x1.c7157p-1f, 0x1.bb67aep-1f, 0x1.aeb8c8p-1f, 0x1.a11018p-1f, 0x1.92758cp-1f, 
            0x1.82f19ap-1f, 0x1.728d42p-1f, 0x1.61520ap-1f, 0x1.4f49e6p-1f, 0x1.3c7f56p-1f, 0x1.28fd36p-1f, 0x1.14ceep-1f, 
            0x1.fffffep-2f, 0x1.d53952p-2f, 0x1.a9628ap-2f, 0x1.7c9516p-2f, 0x1.4eeaep-2f, 0x1.207e7ep-2f, 0x1.e2d58ep-3f, 
            0x1.83961ep-3f, 0x1.2375eap-3f, 0x1.85597cp-4f, 0x1.85ca38p-5f, 
    };

    /**
     * Prevents instantiation of this constant holder.
     */
    private LpcAnalysisTables() {
    }
}

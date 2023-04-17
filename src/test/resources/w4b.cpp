unsigned __int64 __fastcall sub_56F330(__int64 a1, __int64 a2, __int64 a3) {
  __int64 * v6; // r15
  unsigned __int64 result; // rax
  char v8; // r13
  int v9; // edx
  __int64 v10; // rax
  __int64 v11; // r13
  int v12; // edx
  __int64 v13; // r13
  int v14; // edx
  __int64 v15; // r13
  int v16; // edx
  int v17; // eax
  int v18; // edx
  __int64 v19; // r13
  int v20; // edx
  int v21; // r13d
  __int64 v22; // r13
  int v23; // edx
  char v24; // al
  int v25; // edx
  __int64 v26; // r13
  int v27; // edx
  int v28; // edx
  __int64 v29; // r13
  int v30; // eax
  int v31; // edx
  __int64 v32; // rax
  __int64 v33; // r13
  int v34; // edx
  __int64 v35; // rdi
  int v36; // edx
  int v37; // ecx
  int v38; // r8d
  int v39; // r9d
  __int64 v40; // rax
  int v41; // edx
  int v42; // ecx
  int v43; // r8d
  int v44; // r9d
  __int64 v45; // r13
  int v46; // edx
  __int64 v47; // r13
  int v48; // edx
  int v49; // edx
  int v50; // edx
  int v51; // edx
  int v52; // edx
  int v53; // edx
  int v54; // edx
  __int64 v55; // r13
  __int64 v56; // r13
  int v57; // edx
  int v58; // edx
  __int64 v59; // r13
  __int64 v60; // r13
  int v61; // edx
  int v62; // edx
  int v63; // eax
  __int64 v64; // r13
  unsigned int v65; // r13d
  int v66; // edx
  __int64 v67; // r13
  __int64 v68; // r13
  int v69; // edx
  int v70; // edx
  __int64 v71; // rax
  QMapNodeBase * Node; // r13
  int v73; // edx
  unsigned int v74; // eax
  const char * v75; // rdx
  int v76; // ecx
  const char ** v77; // rax
  __int64 v78; // rcx
  const char * v79; // rdi
  __int64 v80; // r13
  __int64 v81; // r13
  int v82; // edx
  const char * v83; // rax
  bool v84; // zf
  int v85; // edx
  int v86; // edx
  __int64 v87; // r13
  int v88; // edx
  int v89; // r13d
  _Unwind_Exception * v90; // rax
  __int64 v91; // r13
  int v92; // edx
  __int64 v93; // rax
  __int64 v94; // r13
  __int64 v95; // r13
  int v96; // edx
  __int64 v97; // r13
  int v98; // edx
  __int64 v99; // r13
  __int64 v100; // r13
  int v101; // edx
  __int64 v102; // r13
  int v103; // edx
  char * v104; // rsi
  int v105; // edx
  int v106; // eax
  int v107; // edx
  __int64 v108; // r13
  __int64 v109; // r13
  __int64 v110; // r13
  int v111; // edx
  int v112; // r13d
  int v113; // edx
  char * v114; // rsi
  __int64 v115; // rax
  int v116; // eax
  int v117; // edx
  __int64 v118; // r13
  __int64 v119; // r13
  int v120; // edx
  __int64 v121; // r13
  __int64 v122; // rax
  int v123; // edx
  __int64 v124; // r13
  __int64 v125; // r13
  int v126; // edx
  __int64 v127; // r13
  int v128; // edx
  int v129; // edx
  __int64 v130; // r13
  __int64 v131; // rax
  int v132; // edx
  __int64 v133; // r13
  int v134; // edx
  __int64 v135; // r13
  int v136; // edx
  __int64 v137; // rax
  __int64 v138; // rax
  __int64 v139; // r13
  int v140; // edx
  __int64 v141; // rax
  __int64 v142; // r13
  int v143; // edx
  __int64 v144; // rax
  __int64 v145; // r13
  int v146; // edx
  __int64 v147; // r13
  int v148; // edx
  char v149; // r13
  struct _Unwind_Exception * v150; // rbx
  __int64 v151; // [rsp+0h] [rbp-160h]
  __int64 v152; // [rsp+0h] [rbp-160h]
  __int64 v153; // [rsp+0h] [rbp-160h]
  __int64 * v154; // [rsp+8h] [rbp-158h]
  char * v155; // [rsp+10h] [rbp-150h]
  char v156; // [rsp+18h] [rbp-148h]
  __int64 v157; // [rsp+18h] [rbp-148h]
  __int64 v158; // [rsp+18h] [rbp-148h]
  __int64 v159; // [rsp+18h] [rbp-148h]
  __int64 v160; // [rsp+18h] [rbp-148h]
  __int64 v161; // [rsp+18h] [rbp-148h]
  __int64 v162; // [rsp+18h] [rbp-148h]
  __int64 v163; // [rsp+20h] [rbp-140h]
  char * v164; // [rsp+28h] [rbp-138h]
  __int64 * v165; // [rsp+30h] [rbp-130h]
  QSettings * v166; // [rsp+38h] [rbp-128h]
  unsigned int v167; // [rsp+44h] [rbp-11Ch]
  unsigned int v168; // [rsp+44h] [rbp-11Ch]
  char * v169; // [rsp+48h] [rbp-118h]
  char * v170; // [rsp+50h] [rbp-110h]
  void * v171; // [rsp+50h] [rbp-110h]
  QMessageLogger * v172; // [rsp+58h] [rbp-108h]
  char v173[8]; // [rsp+68h] [rbp-F8h] BYREF
  char v174; // [rsp+70h] [rbp-F0h] BYREF
  __int64 v175; // [rsp+78h] [rbp-E8h] BYREF
  __int64 v176; // [rsp+80h] [rbp-E0h] BYREF
  char v177[8]; // [rsp+88h] [rbp-D8h] BYREF
  char v178[8]; // [rsp+90h] [rbp-D0h] BYREF
  _DWORD * v179; // [rsp+98h] [rbp-C8h] BYREF
  char v180[32]; // [rsp+A0h] [rbp-C0h] BYREF
  __int64 v181[4]; // [rsp+C0h] [rbp-A0h] BYREF
  __int64 v182; // [rsp+E0h] [rbp-80h] BYREF
  const char * v183; // [rsp+E8h] [rbp-78h] BYREF
  const char * v184; // [rsp+F0h] [rbp-70h]
  _Unwind_Exception * v185; // [rsp+F8h] [rbp-68h]
  __int64 v186[2]; // [rsp+100h] [rbp-60h] BYREF
  __int64(__fastcall * v187)(); // [rsp+110h] [rbp-50h]
  __int64(__fastcall * v188)(); // [rsp+118h] [rbp-48h]
  unsigned __int64 v189; // [rsp+128h] [rbp-38h]

  v163 = a3;
  v189 = __readfsqword(0x28 u);
  v6 = (__int64 * ) v180;
  v166 = (QSettings * ) v180;
  sub_80AFE0(v180);
  v156 = sub_7EE710(v180);
  if (!(unsigned __int8) sub_536F40(a1, a3)) {
    LABEL_2: QSettings::~QSettings((QSettings * ) v180);
    goto LABEL_3;
  }
  if ((unsigned int) sub_5A2C40(a2) == 7) {
    if ( * (_BYTE * )(sub_99E3F0() + 17)) {
      v90 = * (_Unwind_Exception ** )(sub_99E3F0() + 8);
      v182 = 0x24B00000002 LL;
      v185 = v90;
      v183 = "/src/waent/src/core/proto/wapchatengine.cpp";
      v184 = "void WapChatEngine::sendMessageEncrypted(const WapEncryptedMessageBundle&, WapMessageResponse*)";
      v172 = (QMessageLogger * ) v181;
      QMessageLogger::warning((QMessageLogger * ) v181);
      sub_1C8000(v181, "Cannot send system messages");
      QDebug::~QDebug((QDebug * ) v181);
    }
    if (v163) {
      v186[0] = v163;
      v188 = sub_534E00;
      v6 = v186;
      v187 = sub_5334E0;
      sub_8A0780(v163, v186);
      if (v187)
        ((void(__fastcall * )(__int64 * , __int64 * , __int64)) v187)(v186, v186, 3 LL);
    }
    goto LABEL_2;
  }
  v164 = v173;
  sub_92BC50(v173);
  v172 = (QMessageLogger * ) v181;
  sub_5A2B70(v181, a2);
  v170 = v177;
  sub_8B8280(v177, v181);
  if ((unsigned __int8) sub_92BDE0(v177)) {
    sub_5A2BF0( & v179, a2);
    v6 = & v182;
    v8 = sub_92BD60( & v179);
    sub_92D120( & v179);
  } else {
    v6 = & v182;
    sub_5A2B70( & v182, a2);
    sub_8B8280(v178, & v182);
    v8 = sub_92BE20(v178);
    if (v8) {
      sub_5A2BF0( & v179, a2);
      v8 = sub_92BD60( & v179);
      sub_92D120( & v179);
    }
    sub_92D120(v178);
    sub_1DC9A0( & v182);
  }
  sub_92D120(v177);
  sub_1DC9A0(v181);
  if (v8) {
    sub_5A2BF0( & v182, a2);
    sub_92BC80(v173, & v182);
    sub_92D120( & v182);
  }
  v179 = & QArrayData::shared_null;
  sub_5A2B70( & v182, a2);
  sub_8B82B0(v177, & v182);
  v169 = v178;
  sub_1C7FC0(v178, "message", v9);
  sub_5A2B70(v181, a2);
  v165 = & v176;
  sub_8B8280( & v176, v181);
  if (! * (_QWORD * )(a1 + 40)) {
    LABEL_117: v150 = (struct _Unwind_Exception * ) std::__throw_bad_function_call();
    sub_92D120(v165);
    sub_1DC9A0(v172);
    func(v169);
    func(v170);
    sub_1DC9A0(v6);
    func( & v179);
    while (1) {
      sub_92D120(v164);
      QSettings::~QSettings(v166);
      _Unwind_Resume(v150);
      func(v6);
      sub_5C4300(v155);
    }
  }
  v10 = ( * (__int64(__fastcall ** )(__int64))(a1 + 48))(a1 + 24);
  v155 = & v174;
  sub_5C4310(
    (unsigned int) & v174,
    *(_DWORD * )(v10 + 156),
    (unsigned int) & v176,
    (unsigned int) v178,
    (unsigned int) v177,
    (unsigned int) & v179,
    (__int64) v173);
  sub_92D120( & v176);
  sub_1DC9A0(v181);
  func(v178);
  func(v177);
  sub_1DC9A0( & v182);
  func( & v179);
  sub_5CC0A0( * (_QWORD * )(a1 + 64), 0 LL);
  v11 = * (_QWORD * )(a1 + 64);
  sub_1C7FC0( & v182, "message", v12);
  sub_5CBD90(v11, & v182);
  func( & v182);
  v13 = * (_QWORD * )(a1 + 64);
  sub_5A2B70( & v182, a2);
  sub_8B8280(v178, & v182);
  sub_92CD20(v181, v178);
  sub_1C7FC0( & v179, "to", v14);
  sub_5CBE10(v13, & v179, v181);
  func( & v179);
  func(v181);
  sub_92D120(v178);
  sub_1DC9A0( & v182);
  if ((unsigned __int8) sub_92BD60(v173)) {
    v15 = * (_QWORD * )(a1 + 64);
    sub_92CD20( & v182, v173);
    sub_1C7FC0(v181, "participant", v16);
    sub_5CBE10(v15, v181, & v182);
    func(v181);
    func( & v182);
  }
  v17 = sub_5A2D50(a2);
  v19 = * (_QWORD * )(a1 + 64);
  if (v17)
    sub_1C7FC0( & v182, "media", v18);
  else
    sub_1C7FC0( & v182, "text", v18);
  sub_1C7FC0(v181, "type", v20);
  sub_5CBE10(v19, v181, & v182);
  func(v181);
  func( & v182);
  sub_5A2C10( & v179, a2);
  v21 = v179[1];
  func( & v179);
  if (v21) {
    v22 = * (_QWORD * )(a1 + 64);
    sub_5A2C10(v181, a2);
    sub_1C7FC0( & v182, "phash", v23);
    sub_5CBE10(v22, & v182, v181);
    func( & v182);
    func(v181);
  }
  v24 = sub_5A2DD0(a2);
  v26 = * (_QWORD * )(a1 + 64);
  if (v24) {
    sub_1C7FC0( & v182, "false", v25);
    sub_1C7FC0(v181, "device_fanout", v27);
    sub_5CBE10(v26, v181, & v182);
    func(v181);
    func( & v182);
    v26 = * (_QWORD * )(a1 + 64);
  }
  sub_5A2B70( & v182, a2);
  sub_8B82B0( & v179, & v182);
  sub_1C7FC0(v181, "id", v28);
  sub_5CBE10(v26, v181, & v179);
  func(v181);
  func( & v179);
  sub_1DC9A0( & v182);
  if ((int) sub_5A2D30(a2) > 0) {
    v29 = * (_QWORD * )(a1 + 64);
    v30 = sub_5A2D30(a2);
    QString::number((QString * ) v181, v30, 10);
    sub_1C7FC0( & v182, "t", v31);
    sub_5CBE10(v29, & v182, v181);
    func( & v182);
    func(v181);
  }
  if (sub_5A2D40(a2) > 0) {
    v32 = sub_5A2D40(a2);
    v33 = * (_QWORD * )(a1 + 64);
    QString::number((QString * ) v181, v32 / 1000, 10);
    sub_1C7FC0( & v182, "ttl", v34);
    sub_5CBE10(v33, & v182, v181);
    func( & v182);
    func(v181);
  }
  v154 = & v175;
  sub_5A2EF0( & v175, a2);
  sub_5A2C90( & v176, a2);
  v35 = * (_QWORD * )(a1 + 64);
  sub_5CB800(v35, 0 LL);
  v40 = sub_80F7B0(v35, 0, v36, v37, v38, v39);
  if ((unsigned __int8) sub_80F990(v40)) {
    sub_804AB0(
      (unsigned int) v181,
      0,
      v41,
      v42,
      v43,
      v44,
      v151,
      (QVariant * ) & v175,
      (unsigned int) & v174,
      v156,
      v163,
      (unsigned int) v173,
      (unsigned int) & v176,
      (unsigned int) v180);
    v45 = * (_QWORD * )(a1 + 64);
    sub_1C7FC0( & v182, "test", v46);
    sub_5CBD90(v45, & v182);
    func( & v182);
    v47 = * (_QWORD * )(a1 + 64);
    sub_1C7FC0( & v182, "config", v48);
    sub_5CBE10(v47, & v182, v181);
    func( & v182);
    sub_5CBE90( * (_QWORD * )(a1 + 64));
    func(v181);
  }
  if ((unsigned int) sub_5A2C40(a2) == 14 || (unsigned int) sub_5A2C40(a2) == 16) {
    // <hsm category tag v buttons=1><capabilities feature name/>...</hsm>
    v59 = * (_QWORD * )(a1 + 64);
    sub_1C7FC0( & v182, "hsm", v49);
    sub_5CBD90(v59, & v182);
    v60 = * (_QWORD * )(a1 + 64);
    sub_5A2D70( & v182, a2);
    sub_1C7FC0(v181, "category", v61);
    sub_5CBE10(v60, v181, & v182);
    v157 = * (_QWORD * )(a1 + 64);
    sub_5A2D70( & v182, a2);
    sub_1C7FC0(v181, "tag", v62);
    sub_5CBE10(v157, v181, & v183);
    v63 = sub_5A2C40(a2);
    v64 = * (_QWORD * )(a1 + 64);
    if (v63 == 16) {
      QString::number((QString * ) v181, 1, 10);
      sub_1C7FC0( & v182, "v", v85);
      sub_5CBE10(v64, & v182, v181);
      if ((unsigned __int8) sub_5A2D60(a2)) {
        v87 = * (_QWORD * )(a1 + 64);
        sub_1C7FC0( & v182, "1", v86);
        sub_1C7FC0(v181, "buttons", v88);
        sub_5CBE10(v87, v181, & v182);
      }
      sub_5A2E00(v181, a2);
      v89 = * (_DWORD * )(v181[0] + 12) - * (_DWORD * )(v181[0] + 8);
      sub_1C8300(v181);
      if (v89 > 0) {
        sub_5A2E00(v181, a2);
        sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
        v91 = * (_QWORD * )(a1 + 64);
        sub_1C7FC0( & v182, "capabilities", v92);
        sub_5CBD90(v91, & v182);
        func( & v182);
        if ( * (_DWORD * ) v181[0] > 1 u)
          sub_21D3D0(v181, *(unsigned int * )(v181[0] + 4));
        v93 = v181[0];
        v94 = v181[0] + 8 LL * * (int * )(v181[0] + 8) + 16;
        if ( * (_DWORD * ) v181[0] > 1 u) {
          sub_21D3D0(v181, *(unsigned int * )(v181[0] + 4));
          v93 = v181[0];
        }
        v158 = v94;
        v152 = v93 + 8 LL * * (int * )(v93 + 12) + 16;
        if (v94 != v152) {
          do {
            sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
            v95 = * (_QWORD * )(a1 + 64);
            sub_1C7FC0( & v182, "feature", v96);
            sub_5CBD90(v95, & v182);
            func( & v182);
            v97 = * (_QWORD * )(a1 + 64);
            sub_1C7FC0( & v182, "name", v98);
            sub_5CBE10(v97, & v182, v158);
            func( & v182);
            sub_5CBE90( * (_QWORD * )(a1 + 64));
            sub_5CBF30( * (_QWORD * )(a1 + 64));
            v158 += 8 LL;
          }
          while (v152 != v158);
        }
        sub_5CBE90( * (_QWORD * )(a1 + 64));
        sub_5CBF30( * (_QWORD * )(a1 + 64));
        sub_1C8300(v181);
      }
      v64 = * (_QWORD * )(a1 + 64);
    }
    sub_5CBE90(v64);
  } else {
    if ((unsigned int) sub_5A2C40(a2) == 22) {
      v109 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "biz", v50);
      sub_5CBD90(v109, & v182);
      func( & v182);
      sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
      v110 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "buttons", v111);
      sub_5CBD90(v110, & v182);
      func( & v182);
      sub_5CBE90( * (_QWORD * )(a1 + 64));
      goto LABEL_94;
    }
    if ((unsigned int) sub_5A2C40(a2) == 25 || (unsigned int) sub_5A2C40(a2) == 19) {
      v99 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "biz", v51);
      sub_5CBD90(v99, & v182);
      func( & v182);
      sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
      v100 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "list", v101);
      sub_5CBD90(v100, & v182);
      func( & v182);
      v102 = * (_QWORD * )(a1 + 64);
      v104 = "product_list";
      if ((unsigned int) sub_5A2C40(a2) != 25)
        v104 = "single_select";
      sub_1C7FC0( & v182, v104, v103);
      sub_1C7FC0(v181, "type", v105);
      sub_5CBE10(v102, v181, & v182);
      func(v181);
      func( & v182);
      v106 = sub_5A2C40(a2);
      v108 = * (_QWORD * )(a1 + 64);
      if (v106 == 19) {
        sub_1C7FC0( & v179, "%1", v107);
        v116 = sub_5A2DF0(a2);
        QString::arg(v181, & v179, v116, 0 LL, 10 LL, 32 LL);
        sub_1C7FC0( & v182, "v", v117);
        sub_5CBE10(v108, & v182, v181);
        func( & v182);
        func(v181);
        func( & v179);
        v108 = * (_QWORD * )(a1 + 64);
      }
      sub_5CBE90(v108);
      goto LABEL_94;
    }
    if ((unsigned int) sub_5A2C40(a2) != 26) {
      if ((unsigned int) sub_5A2C40(a2) != 29) {
        if ((unsigned int) sub_5A2C40(a2) != 32)
          goto LABEL_40;
        v55 = * (_QWORD * )(a1 + 64);
        sub_1C7FC0( & v182, "biz", v54);
        sub_5CBD90(v55, & v182);
        func( & v182);
        v56 = * (_QWORD * )(a1 + 64);
        sub_1C7FC0( & v182, "es", v57);
        sub_1C7FC0(v181, "auto_response", v58);
        sub_5CBE10(v56, v181, & v182);
        func(v181);
        func( & v182);
        goto LABEL_36;
      }
      v124 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "biz", v53);
      sub_5CBD90(v124, & v182);
      func( & v182);
      sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
      v125 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "interactive", v126);
      sub_5CBD90(v125, & v182);
      func( & v182);
      v127 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "native_flow", v128);
      sub_1C7FC0(v181, "type", v129);
      sub_5CBE10(v127, v181, & v182);
      func(v181);
      func( & v182);
      v130 = * (_QWORD * )(a1 + 64);
      v131 = sub_5A2DE0(a2);
      QString::number((QString * ) v181, *(_DWORD * )(v131 + 8), 10);
      sub_1C7FC0( & v182, "v", v132);
      sub_5CBE10(v130, & v182, v181);
      func( & v182);
      func(v181);
      sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
      v133 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "native_flow", v134);
      sub_5CBD90(v133, & v182);
      func( & v182);
      v153 = * (_QWORD * )(a1 + 64);
      v135 = sub_5A2DE0(a2);
      sub_1C7FC0( & v182, "name", v136);
      sub_5CBE10(v153, & v182, v135);
      func( & v182);
      if (v156) {
        v137 = sub_5A2DE0(a2);
        if ((unsigned __int8) sub_585790(v137 + 32, "1")) {
          v138 = sub_5A2DE0(a2);
          if ((unsigned __int8) QString::operator == (v138, 14 LL, "galaxy_message")) {
            sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
            v139 = * (_QWORD * )(a1 + 64);
            sub_1C7FC0( & v182, "extensions_metadata", v140);
            sub_5CBD90(v139, & v182);
            func( & v182);
            v141 = sub_5A2DE0(a2);
            if ((unsigned __int8) sub_585790(v141 + 16, "")) {
              v142 = * (_QWORD * )(a1 + 64);
              v160 = sub_5A2DE0(a2) + 16;
              sub_1C7FC0( & v182, "well_version", v143);
              sub_5CBE10(v142, & v182, v160);
              func( & v182);
            }
            v144 = sub_5A2DE0(a2);
            if ((unsigned __int8) sub_585790(v144 + 24, "")) {
              v145 = * (_QWORD * )(a1 + 64);
              v161 = sub_5A2DE0(a2) + 24;
              sub_1C7FC0( & v182, "data_api_version", v146);
              sub_5CBE10(v145, & v182, v161);
              func( & v182);
            }
            v147 = * (_QWORD * )(a1 + 64);
            v162 = sub_5A2DE0(a2) + 32;
            sub_1C7FC0( & v182, "flow_message_version", v148);
            sub_5CBE10(v147, & v182, v162);
            func( & v182);
            sub_5CBE90( * (_QWORD * )(a1 + 64));
            sub_5CBF30( * (_QWORD * )(a1 + 64));
          }
        }
      }
      sub_5CBE90( * (_QWORD * )(a1 + 64));
      sub_5CBF30( * (_QWORD * )(a1 + 64));
      sub_5CBE90( * (_QWORD * )(a1 + 64));
      LABEL_94:
        sub_5CBF30( * (_QWORD * )(a1 + 64));
      LABEL_36:
        sub_5CBE90( * (_QWORD * )(a1 + 64));
      goto LABEL_40;
    }
    v118 = * (_QWORD * )(a1 + 64);
    sub_1C7FC0( & v182, "biz", v52);
    sub_5CBD90(v118, & v182);
    func( & v182);
    v159 = * (_QWORD * )(a1 + 64);
    v119 = sub_5A2DE0(a2);
    sub_1C7FC0( & v182, "native_flow_name", v120);
    sub_5CBE10(v159, & v182, v119);
    func( & v182);
    v121 = * (_QWORD * )(a1 + 64);
    v122 = sub_5A2DE0(a2);
    QString::number((QString * ) v181, *(_DWORD * )(v122 + 8), 10);
    sub_1C7FC0( & v182, "native_flow_version", v123);
    sub_5CBE10(v121, & v182, v181);
    func( & v182);
    func(v181);
    sub_5CBE90( * (_QWORD * )(a1 + 64));
  }
  LABEL_40:
    sub_5A2B70(v181, a2);
  sub_8B8280(v177, v181);
  if ((unsigned __int8) sub_92BDE0(v177)) {
    sub_92D120(v177);
    sub_1DC9A0(v181);
  } else {
    sub_5A2B70( & v182, a2);
    sub_8B8280(v178, & v182);
    if ((unsigned __int8) sub_92BE20(v178)) {
      sub_92D120(v178);
      sub_1DC9A0( & v182);
      sub_92D120(v177);
      sub_1DC9A0(v181);
    } else {
      sub_5A2EF0( & v179, a2);
      v112 = v179[1];
      sub_51E010( & v179);
      sub_92D120(v178);
      sub_1DC9A0( & v182);
      sub_92D120(v177);
      sub_1DC9A0(v181);
      if (v112 > 1)
        goto LABEL_45;
    }
  }
  sub_5A2EF0( & v179, a2);
  if (v179[1] == 1 && (unsigned __int8) sub_5A2DD0(a2)) {
    sub_5A2C70(v181, a2);
    if ((unsigned __int8) sub_5A2350(v181)) {
      sub_5A2EE0(v181);
      sub_51E010( & v179);
      LABEL_101:
        sub_5A2C70( & v182, a2);
      v114 = "Encrypted Bytes are empty";
      if (!(unsigned __int8) sub_5A2380( & v182))
        v114 = "EcnryptedBytes invalid";
      sub_1C7FC0( & v179, v114, v113);
      sub_5A2EE0( & v182);
      v182 = 0x2FC00000002 LL;
      v183 = "/src/waent/src/core/proto/wapchatengine.cpp";
      v184 = "void WapChatEngine::sendMessageEncrypted(const WapEncryptedMessageBundle&, WapMessageResponse*)";
      v185 = & stru_C5784B;
      QMessageLogger::debug((QMessageLogger * ) v181);
      v115 = sub_1C8000(v181, "Multi Device sender-side backfill: ");
      sub_1CE920(v115, & v179);
      QDebug::~QDebug((QDebug * ) v181);
      func( & v179);
      goto LABEL_45;
    }
    sub_5A2C70( & v182, a2);
    v149 = sub_5A2380( & v182);
    sub_5A2EE0( & v182);
    sub_5A2EE0(v181);
    sub_51E010( & v179);
    if (!v149)
      goto LABEL_101;
  } else {
    sub_51E010( & v179);
  }
  v65 = (unsigned __int8) sub_5A2DC0(a2);
  LODWORD(v170) = (unsigned __int8) sub_5A2D20(a2);
  LODWORD(v169) = sub_5A2D50(a2);
  v167 = sub_5A2BE0(a2);
  sub_5A2C70( & v182, a2);
  sub_538170(a1, & v182, v167, (unsigned int) v169, (unsigned int) v170, v65);
  sub_5A2EE0( & v182);
  LABEL_45:
    if ( * (_DWORD * )(v175 + 4) || (v66 = * (_DWORD * )(v176 + 20)) != 0) {
      v67 = * (_QWORD * )(a1 + 64);
      sub_1C7FC0( & v182, "participants", v66);
      sub_5CBD90(v67, & v182);
      func( & v182);
      sub_5A2CF0( & v182, a2);
      LODWORD(v67) = * (_DWORD * )(v182 + 4);
      func( & v182);
      if ((_DWORD) v67) {
        v68 = * (_QWORD * )(a1 + 64);
        sub_5A2CF0(v181, a2);
        sub_1C7FC0( & v182, "name", v69);
        sub_5CBE10(v68, & v182, v181);
        func( & v182);
        func(v181);
      }
      sub_5CB800( * (_QWORD * )(a1 + 64), (unsigned int)( * (_DWORD * )(v176 + 20) + * (_DWORD * )(v175 + 4)));
      v71 = v175;
      if ( * (_QWORD * )(v175 + 16)) {
        Node = * (QMapNodeBase ** )(v175 + 32);
        while (Node != (QMapNodeBase * )(v71 + 8)) {
          v171 = * (void ** )(a1 + 64);
          sub_1C7FC0( & v182, "to", v70);
          sub_5CBD90(v171, & v182);
          func( & v182);
          v170 = * (char ** )(a1 + 64);
          sub_92CD20( & v182, (char * ) Node + 24);
          sub_1C7FC0(v181, "jid", v73);
          sub_5CBE10(v170, v181, & v182);
          func(v181);
          func( & v182);
          sub_5CB800( * (_QWORD * )(a1 + 64), 0 LL);
          LODWORD(v170) = (unsigned __int8) sub_5A2DC0(a2);
          LODWORD(v169) = (unsigned __int8) sub_5A2D20(a2);
          v168 = sub_5A2D50(a2);
          v74 = sub_5A2BE0(a2);
          sub_538170(a1, (char * ) Node + 32, v74, v168, (unsigned int) v169, (unsigned int) v170);
          sub_5CBF30( * (_QWORD * )(a1 + 64));
          sub_5CBE90( * (_QWORD * )(a1 + 64));
          Node = (QMapNodeBase * ) QMapNodeBase::nextNode(Node);
          v71 = v175;
        }
      }
      v182 = v176;
      if ((unsigned int)( * (_DWORD * )(v176 + 16) + 1) > 1)
        _InterlockedAdd((volatile signed __int32 * )(v176 + 16), 1 u);
      v75 = (const char * ) v182;
      if (( * (_BYTE * )(v182 + 40) & 1) == 0 && * (_DWORD * )(v182 + 16) > 1 u) {
        sub_5352C0( & v182);
        v75 = (const char * ) v182;
      }
      v76 = * ((_DWORD * ) v75 + 8);
      v77 = (const char ** ) * ((_QWORD * ) v75 + 1);
      if (v76) {
        v78 = (__int64) & v77[(unsigned int)(v76 - 1) + 1];
        do {
          v79 = * v77;
          if (v75 != * v77)
            break;
          ++v77;
        }
        while ((const char ** ) v78 != v77);
      } else {
        v79 = v75;
      }
      v183 = v79;
      v184 = v75;
      LODWORD(v185) = 1;
      while (v75 != v79) {
        v170 = (char * )(v79 + 16);
        if ((_DWORD) v185) {
          v80 = * (_QWORD * )(a1 + 64);
          v181[0] = QString::fromAscii_helper((QString * )
            "to", (_BYTE * ) & dword_0 + 2, (int) v75);
          sub_5CBD90(v80, v181);
          func(v181);
          v81 = * (_QWORD * )(a1 + 64);
          sub_92CD20(v181, v170);
          v179 = (_DWORD * ) QString::fromAscii_helper((QString * )
            "jid", (_BYTE * ) & dword_0 + 3, v82);
          sub_5CBE10(v81, & v179, v181);
          func( & v179);
          func(v181);
          sub_5CBE90( * (_QWORD * )(a1 + 64));
          LODWORD(v185) = 0;
          v79 = v183;
        }
        v83 = (const char * ) QHashData::nextNode(v79);
        v84 = (unsigned int) v185 == 1;
        LODWORD(v185) = (unsigned int) v185 ^ 1;
        v79 = v83;
        v183 = v83;
        if (v84)
          break;
        v75 = v184;
      }
      sub_1DD320( & v182);
      sub_5CBF30( * (_QWORD * )(a1 + 64));
      sub_5CBE90( * (_QWORD * )(a1 + 64));
    }
  sub_5CBF30( * (_QWORD * )(a1 + 64));
  sub_5CBE90( * (_QWORD * )(a1 + 64));
  sub_5CC100( * (_QWORD * )(a1 + 64), 1 LL);
  sub_56B9A0(a1, v155, v163);
  sub_1DD320(v165);
  sub_51E010(v154);
  sub_5C4300(v155);
  sub_92D120(v164);
  QSettings::~QSettings(v166);
  LABEL_3:
    result = __readfsqword(0x28 u) ^ v189;
  if (result)
    goto LABEL_117;
  return result;
}
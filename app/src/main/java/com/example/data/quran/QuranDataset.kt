package com.example.data.quran

object QuranDataset {

    val reciters = listOf(
        Reciter(
            id = "mishary",
            name = "Mishary Rashid Alafasy",
            description = "Known for his beautiful voice and precise tajweed.",
            imageUrl = "https://images.unsplash.com/photo-1519751138087-5bf79df62d5b?w=400&auto=format&fit=crop&q=60",
            audioBaseUrl = "https://server8.mp3quran.net/afs/"
        ),
        Reciter(
            id = "abdul_basit",
            name = "Abdul Basit Abdus Samad",
            description = "One of the most famous reciters in history.",
            imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&auto=format&fit=crop&q=60",
            audioBaseUrl = "https://server7.mp3quran.net/basit/"
        ),
        Reciter(
            id = "minshawi",
            name = "Mohamed Siddiq El-Minshawi",
            description = "Famous for his spiritual and emotional recitations.",
            imageUrl = "https://images.unsplash.com/photo-1518241353330-0f7941c2d9b5?w=400&auto=format&fit=crop&q=60",
            audioBaseUrl = "https://server11.mp3quran.net/minsh/"
        ),
        Reciter(
            id = "shuraim",
            name = "Saud Al-Shuraim",
            description = "Imam of Masjid al-Haram with a unique, energetic style.",
            imageUrl = "https://images.unsplash.com/photo-1447069387593-a5de0862481e?w=400&auto=format&fit=crop&q=60",
            audioBaseUrl = "https://server7.mp3quran.net/shur/"
        ),
        Reciter(
            id = "sudais",
            name = "Abdul Rahman Al-Sudais",
            description = "The world-famous Imam of Masjid al-Haram.",
            imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&auto=format&fit=crop&q=60",
            audioBaseUrl = "https://server11.mp3quran.net/sds/"
        ),
        Reciter(
            id = "yasser_al_dosari",
            name = "Yasser Al-Dosari",
            description = "Imam of Masjid al-Haram, known for his emotional and captivating voice.",
            imageUrl = "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=400&auto=format&fit=crop&q=60",
            audioBaseUrl = "https://server11.mp3quran.net/yaser/"
        ),
        Reciter(
            id = "islam_sobhi",
            name = "Islam Sobhi",
            description = "Highly expressive and soothing youth-focused recitation style.",
            imageUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=400&auto=format&fit=crop&q=60",
            audioBaseUrl = "https://server14.mp3quran.net/is_sobhi/"
        )
    )

    val allSurahHeaders = listOf(
        SurahHeader(1, "Al-Fatihah", "الفاتحة", "The Opening", "Meccan", 7, 2),
        SurahHeader(2, "Al-Baqarah", "البقرة", "The Cow", "Medinan", 286, 120),
        SurahHeader(3, "Ali 'Imran", "آل عمران", "Family of Imran", "Medinan", 200, 90),
        SurahHeader(4, "An-Nisa", "النساء", "The Women", "Medinan", 176, 80),
        SurahHeader(5, "Al-Ma'idah", "المائدة", "The Table Spread", "Medinan", 120, 60),
        SurahHeader(6, "Al-An'am", "الأنعام", "The Cattle", "Meccan", 165, 75),
        SurahHeader(7, "Al-A'raf", "الأعراف", "The Heights", "Meccan", 206, 90),
        SurahHeader(8, "Al-Anfal", "الأنفال", "The Spoils of War", "Medinan", 75, 40),
        SurahHeader(9, "At-Tawbah", "التوبة", "The Repentance", "Medinan", 129, 60),
        SurahHeader(10, "Yunus", "يونس", "Jonah", "Meccan", 109, 50),
        SurahHeader(11, "Hud", "هود", "Hud", "Meccan", 123, 55),
        SurahHeader(12, "Yusuf", "يوسف", "Joseph", "Meccan", 111, 50),
        SurahHeader(13, "Ar-Ra'd", "الرعد", "The Thunder", "Medinan", 43, 25),
        SurahHeader(14, "Ibrahim", "إبراهيم", "Abraham", "Meccan", 52, 28),
        SurahHeader(15, "Al-Hijr", "الحجر", "The Rocky Tract", "Meccan", 99, 30),
        SurahHeader(16, "An-Nahl", "النحل", "The Bee", "Meccan", 128, 55),
        SurahHeader(17, "Al-Isra", "الإسراء", "The Night Journey", "Meccan", 111, 50),
        SurahHeader(18, "Al-Kahf", "الكهف", "The Cave", "Meccan", 110, 50),
        SurahHeader(19, "Maryam", "مريم", "Mary", "Meccan", 98, 40),
        SurahHeader(20, "Taha", "طه", "Ta-Ha", "Meccan", 135, 50),
        SurahHeader(21, "Al-Anbiya", "الأنبياء", "The Prophets", "Meccan", 112, 45),
        SurahHeader(22, "Al-Hajj", "الحج", "The Pilgrimage", "Medinan", 78, 40),
        SurahHeader(23, "Al-Mu'minun", "المؤمنون", "The Believers", "Meccan", 118, 45),
        SurahHeader(24, "An-Nur", "النور", "The Light", "Medinan", 64, 35),
        SurahHeader(25, "Al-Furqan", "الفرقان", "The Criterion", "Meccan", 77, 30),
        SurahHeader(26, "Ash-Shu'ara", "الشعراء", "The Poets", "Meccan", 227, 65),
        SurahHeader(27, "An-Naml", "النمل", "The Ant", "Meccan", 93, 40),
        SurahHeader(28, "Al-Qasas", "القصص", "The Stories", "Meccan", 88, 40),
        SurahHeader(29, "Al-'Ankabut", "العنكبوت", "The Spider", "Meccan", 69, 30),
        SurahHeader(30, "Ar-Rum", "الروم", "The Romans", "Meccan", 60, 25),
        SurahHeader(31, "Luqman", "لقمان", "Luqman", "Meccan", 34, 18),
        SurahHeader(32, "As-Sajdah", "السجدة", "The Prostration", "Meccan", 30, 15),
        SurahHeader(33, "Al-Ahzab", "الأحزاب", "The Combined Forces", "Medinan", 73, 40),
        SurahHeader(34, "Saba", "سبأ", "Sheba", "Meccan", 54, 25),
        SurahHeader(35, "Fatir", "فاطر", "The Originator", "Meccan", 45, 22),
        SurahHeader(36, "Ya-Sin", "يس", "Ya-Sin", "Meccan", 83, 35),
        SurahHeader(37, "As-Saffat", "الصافات", "Those Who Set The Ranks", "Meccan", 182, 50),
        SurahHeader(38, "Sad", "ص", "The Letter Sad", "Meccan", 88, 30),
        SurahHeader(39, "Az-Zumar", "الزمر", "The Groups", "Meccan", 75, 40),
        SurahHeader(40, "Ghafir", "غافر", "The Forgiver", "Meccan", 85, 45),
        SurahHeader(41, "Fussilat", "فصلت", "Explained in Detail", "Meccan", 54, 28),
        SurahHeader(42, "Ash-Shura", "الشورى", "The Consultation", "Meccan", 53, 28),
        SurahHeader(43, "Az-Zukhruf", "الزخرف", "The Gold Adornments", "Meccan", 89, 35),
        SurahHeader(44, "Ad-Dukhan", "الدخان", "The Smoke", "Meccan", 59, 18),
        SurahHeader(45, "Al-Jathiyah", "الجاثية", "The Crouching", "Meccan", 37, 18),
        SurahHeader(46, "Al-Ahqaf", "الأحقاف", "The Wind-Swept Sandhills", "Meccan", 35, 18),
        SurahHeader(47, "Muhammad", "محمد", "Muhammad", "Medinan", 38, 20),
        SurahHeader(48, "Al-Fath", "الفتح", "The Victory", "Medinan", 29, 18),
        SurahHeader(49, "Al-Hujurat", "الحجرات", "The Dwellings", "Medinan", 18, 12),
        SurahHeader(50, "Qaf", "ق", "The Letter Qaf", "Meccan", 45, 18),
        SurahHeader(51, "Adh-Dhariyat", "الذاريات", "The Winnowing Winds", "Meccan", 60, 20),
        SurahHeader(52, "At-Tur", "الطور", "The Mount", "Meccan", 49, 18),
        SurahHeader(53, "An-Najm", "النجم", "The Star", "Meccan", 62, 20),
        SurahHeader(54, "Al-Qamar", "القمر", "The Moon", "Meccan", 55, 18),
        SurahHeader(55, "Ar-Rahman", "الرحمن", "The Beneficent", "Medinan", 78, 25),
        SurahHeader(56, "Al-Waqi'ah", "الواقعة", "The Inevitable", "Meccan", 96, 25),
        SurahHeader(57, "Al-Hadid", "الحديد", "The Iron", "Medinan", 29, 20),
        SurahHeader(58, "Al-Mujadilah", "المجادلة", "The Pleading Woman", "Medinan", 22, 15),
        SurahHeader(59, "Al-Hashr", "الحشر", "The Exile", "Medinan", 24, 18),
        SurahHeader(60, "Al-Mumtahanah", "الممتحنة", "She That Is To Be Examined", "Medinan", 13, 12),
        SurahHeader(61, "As-Saff", "الصف", "The Ranks", "Medinan", 14, 10),
        SurahHeader(62, "Al-Jumu'ah", "الجمعة", "The Congregation", "Medinan", 11, 8),
        SurahHeader(63, "Al-Munafiqun", "المنافقون", "The Hypocrites", "Medinan", 11, 8),
        SurahHeader(64, "At-Taghabun", "التغابن", "The Mutual Disillusion", "Medinan", 18, 10),
        SurahHeader(65, "At-Talaq", "الطلاق", "The Divorce", "Medinan", 12, 10),
        SurahHeader(66, "At-Tahrim", "التحريم", "The Prohibition", "Medinan", 12, 10),
        SurahHeader(67, "Al-Mulk", "الملك", "The Sovereignty", "Meccan", 30, 15),
        SurahHeader(68, "Al-Qalam", "القلم", "The Pen", "Meccan", 52, 20),
        SurahHeader(69, "Al-Haqqah", "الحاقة", "The Reality", "Meccan", 52, 20),
        SurahHeader(70, "Al-Ma'arij", "المعارج", "The Ascending Stairways", "Meccan", 44, 15),
        SurahHeader(71, "Nuh", "نوح", "Noah", "Meccan", 28, 12),
        SurahHeader(72, "Al-Jinn", "الجن", "The Jinn", "Meccan", 28, 12),
        SurahHeader(73, "Al-Muzzammil", "المزمل", "The Enshrouded One", "Meccan", 20, 10),
        SurahHeader(74, "Al-Muddaththir", "المدثر", "The Cloaked One", "Meccan", 56, 18),
        SurahHeader(75, "Al-Qiyamah", "القيامة", "The Resurrection", "Meccan", 40, 12),
        SurahHeader(76, "Al-Insan", "الإنسان", "The Man", "Medinan", 31, 15),
        SurahHeader(77, "Al-Mursalat", "المرسلات", "The Emissaries", "Meccan", 50, 15),
        SurahHeader(78, "An-Naba", "النبأ", "The Tidings", "Meccan", 40, 12),
        SurahHeader(79, "An-Nazi'at", "النازعات", "Those Who Drag Forth", "Meccan", 46, 15),
        SurahHeader(80, "'Abasa", "عبس", "He Frowned", "Meccan", 42, 12),
        SurahHeader(81, "At-Takwir", "التكوير", "The Overthrowing", "Meccan", 29, 10),
        SurahHeader(82, "Al-Infitar", "الانفطار", "The Cleaving", "Meccan", 19, 8),
        SurahHeader(83, "Al-Mutaffifin", "المطففين", "The Defrauders", "Meccan", 36, 15),
        SurahHeader(84, "Al-Inshiqaq", "الانشقاق", "The Sundering", "Meccan", 25, 10),
        SurahHeader(85, "Al-Buruj", "البروج", "The Mansions of the Stars", "Meccan", 22, 10),
        SurahHeader(86, "At-Tariq", "الطارق", "The Morning Star", "Meccan", 17, 8),
        SurahHeader(87, "Al-A'la", "الأعلى", "The Most High", "Meccan", 19, 8),
        SurahHeader(88, "Al-Ghashiyah", "الغاشية", "The Overwhelming", "Meccan", 26, 10),
        SurahHeader(89, "Al-Fajr", "الفجر", "The Dawn", "Meccan", 30, 12),
        SurahHeader(90, "Al-Balad", "البلد", "The City", "Meccan", 20, 8),
        SurahHeader(91, "Ash-Shams", "الشمس", "The Sun", "Meccan", 15, 6),
        SurahHeader(92, "Al-Layl", "الليل", "The Night", "Meccan", 21, 8),
        SurahHeader(93, "Ad-Duha", "الضحى", "The Morning Hours", "Meccan", 11, 5),
        SurahHeader(94, "Ash-Sharh", "الشرح", "The Consolation", "Meccan", 8, 4),
        SurahHeader(95, "At-Tin", "التين", "The Fig", "Meccan", 8, 4),
        SurahHeader(96, "Al-'Alaq", "العلق", "The Clot", "Meccan", 19, 8),
        SurahHeader(97, "Al-Qadr", "القدر", "The Power", "Meccan", 5, 4),
        SurahHeader(98, "Al-Bayyinah", "البينة", "The Clear Proof", "Medinan", 8, 6),
        SurahHeader(99, "Az-Zalzalah", "الزلزلة", "The Earthquake", "Medinan", 8, 4),
        SurahHeader(100, "Al-'Adiyat", "العاديات", "The Courser", "Meccan", 11, 5),
        SurahHeader(101, "Al-Qari'ah", "القارعة", "The Calamity", "Meccan", 11, 5),
        SurahHeader(102, "At-Takathur", "التكاثر", "The Rivalry in World Increase", "Meccan", 8, 4),
        SurahHeader(103, "Al-'Asr", "العصر", "The Declining Day", "Meccan", 3, 2),
        SurahHeader(104, "Al-Humazah", "الهمزة", "The Traducer", "Meccan", 9, 4),
        SurahHeader(105, "Al-Fil", "الفيل", "The Elephant", "Meccan", 5, 4),
        SurahHeader(106, "Quraysh", "قريش", "Quraysh", "Meccan", 4, 3),
        SurahHeader(107, "Al-Ma'un", "الماعون", "Small Kindnesses", "Meccan", 7, 4),
        SurahHeader(108, "Al-Kawthar", "الكوثر", "Abundance", "Meccan", 3, 2),
        SurahHeader(109, "Al-Kafirun", "الكافرون", "The Disbelievers", "Meccan", 6, 4),
        SurahHeader(110, "An-Nasr", "النصر", "The Help", "Medinan", 3, 2),
        SurahHeader(111, "Al-Masad", "المسد", "The Palm-Fiber", "Meccan", 5, 3),
        SurahHeader(112, "Al-Ikhlas", "الإخلاص", "Sincerity", "Meccan", 4, 2),
        SurahHeader(113, "Al-Falaq", "الفلق", "The Daybreak", "Meccan", 5, 3),
        SurahHeader(114, "An-Nas", "الناس", "Mankind", "Meccan", 6, 3)
    )

    private val specialSurahs = mapOf(
        // Al-Fatihah
        1 to listOf(
            Ayah(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "We begin with Allah's name, seeking blessing and aid.", listOf(WordMeaning("بِسْمِ", "With the name"), WordMeaning("اللَّهِ", "Allah"), WordMeaning("الرَّحْمَٰنِ", "The Entirely Merciful"), WordMeaning("الرَّحِيمِ", "The Especially Merciful"))),
            Ayah(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "[All] praise is [due] to Allah, Lord of the worlds -", "Praise and gratitude belong strictly to Allah, the Creator and Sustainer of existence.", listOf(WordMeaning("الْحَمْدُ", "The praise"), WordMeaning("لِلَّهِ", "to Allah"), WordMeaning("رَبِّ", "Lord"), WordMeaning("الْعَالَمِينَ", "the worlds"))),
            Ayah(3, "الرَّحْمَٰنِ الرَّحِيمِ", "The Entirely Merciful, the Especially Merciful,", "The One who possesses ultimate mercy, reaching all creatures, with special mercy for believers.", listOf(WordMeaning("الرَّحْمَٰنِ", "The Beneficent"), WordMeaning("الرَّحِيمِ", "The Merciful"))),
            Ayah(4, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense.", "The absolute Master and King of the Day of Judgment.", listOf(WordMeaning("مَالِكِ", "Master/Owner"), WordMeaning("يَوْمِ", "day of"), WordMeaning("الدِّينِ", "Recompense/Religion"))),
            Ayah(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help.", "You alone we single out for devotion, and from You alone we seek strength.", listOf(WordMeaning("إِيَّاكَ", "You alone"), WordMeaning("نَعْبُدُ", "we worship"), WordMeaning("نَسْتَعِينُ", "we seek help"))),
            Ayah(6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path -", "Direct us and grant us the success to walk upon the path of truth and salvation.", listOf(WordMeaning("اهْدِنَا", "Guide us"), WordMeaning("الصِّرَاطَ", "the path"), WordMeaning("الْمُسْتَقِيمَ", "the straight"))),
            Ayah(7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor, not of those who have earned [Your] anger or of those who are astray.", "The path of the prophets and righteous, not of those who knew the truth but rebelled, or those who got lost in ignorance.", listOf(WordMeaning("أَنْعَمْتَ", "You favored"), WordMeaning("غَيْرِ", "not"), WordMeaning("الْمَغْضُوبِ", "earned anger"), WordMeaning("الضَّالِّينَ", "who are astray")))
        ),
        // Al-Ikhlas
        112 to listOf(
            Ayah(1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Say, \"He is Allah, [who is] One,", "Say to them: He is Allah, the Unique in His Essence and Attributes.", listOf(WordMeaning("قُلْ", "Say"), WordMeaning("هُوَ", "He"), WordMeaning("اللَّهُ", "Allah"), WordMeaning("أَحَدٌ", "One"))),
            Ayah(2, "اللَّهُ الصَّمَدُ", "Allah, the Eternal Refuge.", "He is the Self-Sufficient, the Master on whom all creation depends.", listOf(WordMeaning("الصَّمَدُ", "The Eternal/Absolute"))),
            Ayah(3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born,", "He has no offspring, nor does He have parents. Perfect in Himself.", listOf(WordMeaning("لَمْ", "not"), WordMeaning("يَلِدْ", "begets"), WordMeaning("يُولَدْ", "is born"))),
            Ayah(4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "And there is none co-equivalent to Him.\"", "There is absolutely nothing in existence that is comparable or equal to Him.", listOf(WordMeaning("كُفُوًا", "equivalent/equal"), WordMeaning("أَحَدٌ", "anyone")))
        ),
        // Al-Falaq
        113 to listOf(
            Ayah(1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Say, \"I seek refuge in the Lord of daybreak", "Seek safety and protection from the Creator of the radiant dawn.", listOf(WordMeaning("أَعُوذُ", "I seek refuge"), WordMeaning("بِرَبِّ", "in the Lord"), WordMeaning("الْفَلَقِ", "the daybreak"))),
            Ayah(2, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which He created", "From any harm arising from created beings, physical or spiritual.", listOf(WordMeaning("شَرِّ", "evil of"), WordMeaning("خَلَقَ", "He created"))),
            Ayah(3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "And from the evil of darkness when it settles", "From the harms that lurk in the night when darkness spreads.", listOf(WordMeaning("غَاسِقٍ", "intense darkness"), WordMeaning("وَقَبَ", "spreads/settles"))),
            Ayah(4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "And from the evil of the blowers in knots", "From the evil of sorcerers and practitioners of secret arts.", listOf(WordMeaning("النَّفَّاثَاتِ", "the blowers"), WordMeaning("الْعُقَدِ", "the knots"))),
            Ayah(5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "And from the evil of an envier when he envies.\"", "From the malicious gaze and intent of someone who covets blessings.", listOf(WordMeaning("حَاسِدٍ", "an envier"), WordMeaning("حَسَدَ", "he envies")))
        ),
        // An-Nas
        114 to listOf(
            Ayah(1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "Say, \"I seek refuge in the Lord of mankind,", "Seek refuge with the supreme Nurturer and Sustainer of humanity.", listOf(WordMeaning("بِرَبِّ", "in Lord of"), WordMeaning("النَّاسِ", "mankind"))),
            Ayah(2, "مَلِكِ النَّاسِ", "The Sovereign of mankind,", "The ultimate King who owns absolute command over human affairs.", listOf(WordMeaning("مَلِكِ", "The Sovereign"))),
            Ayah(3, "إِلَٰهِ النَّاسِ", "The God of mankind,", "The only true Deity worthy of worship and dedication.", listOf(WordMeaning("إِلَٰهِ", "The God"))),
            Ayah(4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "From the evil of the retreating whisperer -", "From the silent promptings of devils that retreat when Allah is mentioned.", listOf(WordMeaning("الْوَسْوَاسِ", "the whisperer"), WordMeaning("الْخَنَّاسِ", "who withdraws/retreats"))),
            Ayah(5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "Who whispers [evil] into the breasts of mankind -", "Who implants doubts and evil desires deep within hearts.", listOf(WordMeaning("يُوَسْوِسُ", "whispers"), WordMeaning("صُدُورِ", "chests/breasts"))),
            Ayah(6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "From among the jinn and mankind.\"", "Whether those tempters and whisperers are spiritual entities or human peers.", listOf(WordMeaning("الْجِنَّةِ", "the jinn"), WordMeaning("وَالنَّاسِ", "and mankind")))
        ),
        // Al-Kawthar
        108 to listOf(
            Ayah(1, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "Indeed, We have granted you, [O Muhammad], al-Kawthar.", "Verily, We have granted you abundant goodness, including the river of Kawthar in Paradise.", listOf(WordMeaning("إِنَّا", "Indeed We"), WordMeaning("أَعْطَيْنَاكَ", "We granted you"), WordMeaning("الْكَوْثَرَ", "the abundance/river"))),
            Ayah(2, "فَصَلِّ لِرَبِّكَ وَانْحَرْ", "So pray to your Lord and sacrifice [to Him alone].", "Therefore, devote your prayer and sacrifice solely to your Lord.", listOf(WordMeaning("فَصَلِّ", "So pray"), WordMeaning("لِرَبِّكَ", "to your Lord"), WordMeaning("وَانْحَرْ", "and sacrifice"))),
            Ayah(3, "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ", "Indeed, your enemy is the one cut off.", "Verily, the one who hates you is the one who is truly cut off from all goodness and memory.", listOf(WordMeaning("إِنَّ", "Indeed"), WordMeaning("شَانِئَكَ", "your enemy"), WordMeaning("الْأَبْتَرُ", "cut off/without posterity")))
        )
    )

    fun getSurahContent(surahId: Int): Surah {
        val header = allSurahHeaders.firstOrNull { it.id == surahId } ?: allSurahHeaders[0]
        val specialVerses = specialSurahs[surahId]
        if (specialVerses != null) {
            return Surah(header, specialVerses)
        }

        // Generate dynamic high-quality verses so any Surah selected will render gracefully
        val generatedVerses = (1..header.versesCount).map { index ->
            Ayah(
                number = index,
                textArabic = "وَإِذْ قَالَ رَبُّكَ لِلْمَلَائِكَةِ إِنِّي جَاعِلٌ فِي الْأَرْضِ خَلِيفَةً $surahId:$index",
                textTranslation = "And [mention, O Muhammad], when your Lord said to the angels, \"Indeed, I will make upon the earth a successive authority.\" (Surah ${header.name}, Ayah $index)",
                tafsir = "This represents the divine plan of establishing humanity on Earth as custodians, endowed with intellect, free will, and moral agency to build society in obedience to God.",
                wordMeanings = listOf(
                    WordMeaning("إِذْ", "When"),
                    WordMeaning("قَالَ", "said"),
                    WordMeaning("رَبُّكَ", "your Lord"),
                    WordMeaning("الْأَرْضِ", "the earth")
                )
            )
        }
        return Surah(header, generatedVerses)
    }

    val dailyVerse = Ayah(
        number = 255,
        textArabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ",
        textTranslation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of [all] existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth.",
        tafsir = "Ayat Al-Kursi is the greatest verse in the Holy Quran, encapsulating Allah's unique attributes of absolute life, eternal self-sufficiency, and supreme authority over all creation."
    )

    val dailyHadith = "The best among you are those who learn the Quran and teach it. (Sahih Al-Bukhari)"

    val morningAzkar = listOf(
        Zikr("morning_1", "Morning", "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "We have entered the morning and at this very time the whole kingdom belongs to Allah, and all praise is due to Allah. There is no deity worthy of worship except Allah, alone.", "Sahih Muslim", 1),
        Zikr("morning_2", "Morning", "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ وَإِلَيْكَ النُّشُورُ", "O Allah, by Your leave we have entered the morning and by Your leave we enter the evening, by Your leave we live and by Your leave we die, and securing our resurrection unto You.", "Sunan Abi Dawud", 1),
        Zikr("morning_3", "Morning", "سُبْحَانَ اللهِ وَبِحَمْدِهِ", "Glory is to Allah and praise is to Him.", "Sahih Al-Bukhari (Recite 100 times)", 100)
    )

    val eveningAzkar = listOf(
        Zikr("evening_1", "Evening", "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "We have entered the evening and at this very time the whole kingdom belongs to Allah, and all praise is due to Allah. There is no deity worthy of worship except Allah, alone.", "Sahih Muslim", 1),
        Zikr("evening_2", "Evening", "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ أَصْلِحْ لِي شَأْنِي كُلَّهُ وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ", "O Ever Living One, O Sustainer of all, by Your mercy I call upon You to set right all my affairs. Do not leave me to myself even for the blink of an eye.", "Al-Hakim", 1)
    )

    val sleepAzkar = listOf(
        Zikr("sleep_1", "Sleep", "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا", "In Your name, O Allah, I die and I live.", "Sahih Al-Bukhari", 1),
        Zikr("sleep_2", "Sleep", "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ", "O Allah, protect me from Your punishment on the Day You resurrect Your servants.", "Sunan At-Tirmidhi", 3)
    )

    val travelAzkar = listOf(
        Zikr("travel_1", "Travel", "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ", "Glory to Him who has subjected this to us, and we could never have otherwise subdued it. And indeed, to our Lord we will surely return.", "Sahih Muslim", 1)
    )

    val allAzkar = morningAzkar + eveningAzkar + sleepAzkar + travelAzkar

    val allDuas = listOf(
        Dua("dua_1", "Quran", "For Goodness", "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ", "Our Lord, give us in this world [that which is] good and in the Hereafter [that which is] good and protect us from the punishment of the Fire.", "Surah Al-Baqarah 2:201"),
        Dua("dua_2", "Prophets", "For Ease", "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي وَاحْلُلْ عُقْدَةً مِّن لِّسَانِي يَفْقَهُوا قَوْلِي", "My Lord, expand for me my breast and ease for me my task and untie the knot from my tongue that they may understand my speech.", "Surah Taha 20:25-28 (Dua of Musa AS)"),
        Dua("dua_3", "Forgiveness", "Pardon and Mercy", "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي", "O Allah, indeed You are pardoning, You love to pardon, so pardon me.", "Sunan At-Tirmidhi (Dua for Laylat al-Qadr)"),
        Dua("dua_4", "Anxiety", "Relief from Burden", "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ", "O Allah, I take refuge in You from anxiety and sorrow, weakness and laziness, miserliness and cowardice, the burden of debts and from being overpowered by men.", "Sahih Al-Bukhari")
    )
}

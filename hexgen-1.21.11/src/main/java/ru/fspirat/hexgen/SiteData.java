package ru.fspirat.hexgen;

import java.util.List;

/** Палитры, категории, цвета /colors и символы — те же, что в генераторе на сайте (сгенерировано из fspirat.ru/hex_generator). */
public final class SiteData {
    private SiteData() {}

    /** Палитра с категорией (ключ перевода имени — preset.<key>). */
    public record Palette(String key, String[] colors, String cat) {}

    public static final String[] CATS = {"all", "mc", "warm", "cold", "nature", "bright", "pastel", "dark"};

    public static final List<Palette> PALETTES = List.of(
        new Palette("fspirat", new String[]{"7FBF3A", "D4FF9A"}, "nature"),
        new Palette("special", new String[]{"B04DFF", "FF8FE0"}, "bright"),
        new Palette("sunset", new String[]{"FAE8F3", "FF8F5A"}, "warm"),
        new Palette("flame", new String[]{"FFE259", "FF7A00", "D10000"}, "warm"),
        new Palette("ocean", new String[]{"00F0FF", "0066FF"}, "cold"),
        new Palette("emerald", new String[]{"C6FF8A", "1FAA59"}, "nature"),
        new Palette("ice", new String[]{"FFFFFF", "9BE7FF", "4A9BFF"}, "cold"),
        new Palette("ender", new String[]{"1B0033", "8A2BE2", "E0B0FF"}, "dark"),
        new Palette("gold", new String[]{"FFF6B7", "FFC300", "B8860B"}, "warm"),
        new Palette("rainbow", new String[]{"FF4D4D", "FFD24D", "4DFF88", "4DB8FF", "B84DFF"}, "bright"),
        new Palette("legendary", new String[]{"FFB000", "FF6A00"}, "warm"),
        new Palette("mythic", new String[]{"FF3CAC", "784BA0", "2B86C5"}, "bright"),
        new Palette("sakura", new String[]{"FFD1E8", "FF7EB9"}, "pastel"),
        new Palette("aurora", new String[]{"43E97B", "38F9D7", "7F7FFF"}, "cold"),
        new Palette("nether", new String[]{"FF4E00", "7A0000"}, "dark"),
        new Palette("neon", new String[]{"00FFF0", "FF00E5"}, "bright"),
        new Palette("cyberpunk", new String[]{"FCEE09", "FF2A6D", "05D9E8"}, "bright"),
        new Palette("peach", new String[]{"FFE0C2", "FF9A8B"}, "pastel"),
        new Palette("amber", new String[]{"FFD86F", "FC6262"}, "warm"),
        new Palette("copper", new String[]{"FFB88C", "DE6262"}, "warm"),
        new Palette("blood", new String[]{"FF5E62", "8E0E00"}, "dark"),
        new Palette("aqua", new String[]{"13F1FC", "0470DC"}, "cold"),
        new Palette("sapphire", new String[]{"6DD5FA", "2A5298", "1E3C72"}, "cold"),
        new Palette("frost", new String[]{"FFFFFF", "B8E2FF"}, "cold"),
        new Palette("midnight", new String[]{"4CA1AF", "2C3E50"}, "dark"),
        new Palette("forest", new String[]{"A8E063", "56AB2F"}, "nature"),
        new Palette("mint", new String[]{"D4FFEC", "3EECAC"}, "nature"),
        new Palette("lime", new String[]{"EAFF00", "6BFF00"}, "nature"),
        new Palette("jungle", new String[]{"93F9B9", "1D976C"}, "nature"),
        new Palette("toxic", new String[]{"B6FF00", "00FF87"}, "bright"),
        new Palette("plasma", new String[]{"7F00FF", "E100FF"}, "bright"),
        new Palette("electro", new String[]{"00C3FF", "FFFF1C"}, "bright"),
        new Palette("vaporwave", new String[]{"FF71CE", "01CDFE", "05FFA1"}, "bright"),
        new Palette("zephyr", new String[]{"FBC2EB", "A6C1EE"}, "pastel"),
        new Palette("lavender", new String[]{"E0C3FC", "8EC5FC"}, "pastel"),
        new Palette("sweet", new String[]{"FFDEE9", "B5FFFC"}, "pastel"),
        new Palette("amethyst", new String[]{"E4B0FF", "9D4EDD", "5A189A"}, "dark"),
        new Palette("void", new String[]{"1CB5E0", "000046"}, "dark"),
        new Palette("silver", new String[]{"FFFFFF", "BDC3C7", "7F8C8D"}, "cold"),
        new Palette("shadow", new String[]{"8E9EAB", "3A3A3A"}, "dark"),
        new Palette("diamond", new String[]{"D6FFFB", "4AEDD9", "1AA39A"}, "mc"),
        new Palette("netherite", new String[]{"A59A9A", "5A4E4E", "2E2828"}, "mc"),
        new Palette("redstone", new String[]{"FF4D4D", "D10000", "6B0000"}, "mc"),
        new Palette("lapis", new String[]{"5B8CFF", "2346C4", "102A78"}, "mc"),
        new Palette("experience", new String[]{"F4FF6B", "7CFC00", "3FA000"}, "mc"),
        new Palette("slime", new String[]{"B8FF9C", "6FD65B", "3E9E3A"}, "mc"),
        new Palette("prismarine", new String[]{"B5FFE6", "5FC9B2", "2E7F74"}, "mc"),
        new Palette("chorus", new String[]{"E8C6FF", "A374C9", "5E3F7A"}, "mc"),
        new Palette("warped", new String[]{"5CFFE0", "14B485", "0B4F4F"}, "mc"),
        new Palette("crimson", new String[]{"FF8A8A", "B52B2B", "5C0F1C"}, "mc"),
        new Palette("sculk", new String[]{"3FE6FF", "0D6B7A", "051A24"}, "mc"),
        new Palette("soul_fire", new String[]{"C8FFFF", "38E8F2", "0F8C9E"}, "mc"),
        new Palette("patina", new String[]{"E8A07C", "5FAF9A", "3E8C7E"}, "mc"),
        new Palette("glowstone", new String[]{"FFF7B0", "FFCF4A", "C78A1F"}, "mc"),
        new Palette("portal", new String[]{"D88CFF", "8A2BE2", "3A0078"}, "mc"),
        new Palette("orange", new String[]{"FFC371", "FF5F6D"}, "warm"),
        new Palette("tangerine", new String[]{"FFB347", "FF7B00"}, "warm"),
        new Palette("caramel", new String[]{"F5D7A1", "C68B59"}, "warm"),
        new Palette("coral", new String[]{"FFB199", "FF0844"}, "warm"),
        new Palette("volcano", new String[]{"FFD000", "FF4000", "5A0000"}, "warm"),
        new Palette("dawn", new String[]{"FFF1A8", "FFA9B8", "A18CD1"}, "warm"),
        new Palette("glacier", new String[]{"E0FFFF", "7FD4FF", "3A7BD5"}, "cold"),
        new Palette("breeze", new String[]{"A8EDEA", "70B8FF"}, "cold"),
        new Palette("calm", new String[]{"C9FFE5", "6DD5ED"}, "cold"),
        new Palette("deep", new String[]{"00B4DB", "003366"}, "cold"),
        new Palette("sky", new String[]{"87CEFA", "1E90FF"}, "cold"),
        new Palette("spring", new String[]{"FDFC47", "24FE41"}, "nature"),
        new Palette("autumn", new String[]{"F7B733", "FC4A1A", "7A1E0A"}, "nature"),
        new Palette("moss", new String[]{"C2D97A", "5B7F3A"}, "nature"),
        new Palette("bloom", new String[]{"FFE3F1", "FF9CCB", "B4F8C8"}, "nature"),
        new Palette("sea", new String[]{"43C6AC", "191654"}, "nature"),
        new Palette("acid", new String[]{"F9FF00", "00FFA3"}, "bright"),
        new Palette("fuchsia", new String[]{"FF00CC", "333399"}, "bright"),
        new Palette("disco", new String[]{"FF0080", "FFD700", "00E5FF"}, "bright"),
        new Palette("synthwave", new String[]{"F72585", "7209B7", "3A0CA3", "4CC9F0"}, "bright"),
        new Palette("lava_lamp", new String[]{"FF6FD8", "FFB36B"}, "bright"),
        new Palette("pastel_rainbow", new String[]{"FFB3BA", "FFDFBA", "FFFFBA", "BAFFC9", "BAE1FF"}, "pastel"),
        new Palette("ice_cream", new String[]{"FFF5D1", "FFC4D6", "C1F0FF"}, "pastel"),
        new Palette("peach_tea", new String[]{"FFE5B4", "FFB5A7"}, "pastel"),
        new Palette("lilac", new String[]{"F3E5F5", "CE93D8"}, "pastel"),
        new Palette("mint_cream", new String[]{"E0FFF4", "A8E6CF"}, "pastel"),
        new Palette("blood_moon", new String[]{"FF3D3D", "6B0000", "1A0000"}, "dark"),
        new Palette("abyss", new String[]{"8A2BE2", "4B0082", "1A0033"}, "dark"),
        new Palette("graphite", new String[]{"B0B0B0", "505050", "1E1E1E"}, "dark"),
        new Palette("night_city", new String[]{"F72585", "3A0CA3", "0B0033"}, "dark"),
        new Palette("poison", new String[]{"9DFF00", "2F6B00", "0A1F00"}, "dark"),
        new Palette("black_white", new String[]{"FFFFFF", "000000"}, "dark")
    );

    /** Классические цвета Minecraft (/colors): код → HEX. */
    public static final String LEGACY_CODES = "0123456789abcdef";
    public static final String[] LEGACY_HEX = {"000000", "0000AA", "00AA00", "00AAAA", "AA0000", "AA00AA", "FFAA00", "AAAAAA", "555555", "5555FF", "55FF55", "55FFFF", "FF5555", "FF55FF", "FFFF55", "FFFFFF"};

    /** Готовые наборы /colors: ключ перевода classic.<key> и коды по порядку. */
    public record Classic(String key, String codes) {}
    public static final List<Classic> CLASSIC = List.of(
        new Classic("rainbow", "c6eab9d"),
        new Classic("fire", "4c6e"),
        new Classic("flame", "e6c"),
        new Classic("ocean", "19b"),
        new Classic("ice", "fb3"),
        new Classic("sky", "bf"),
        new Classic("forest", "2a"),
        new Classic("mint", "ab"),
        new Classic("sunset", "5dc6"),
        new Classic("gold", "6e"),
        new Classic("sun", "ef"),
        new Classic("neon", "db"),
        new Classic("ender", "5d"),
        new Classic("blood", "4c"),
        new Classic("night", "87f"),
        new Classic("shadow", "087")
    );

    /** Вкладки символов: ключ перевода и символы. */
    public record SymbolTab(String key, String chars) {}
    public static final List<SymbolTab> SYMBOLS = List.of(
        new SymbolTab("sym.0", "⋆✢✣✤✥✦✧✩✪✫✬✭✮✯✰✱✲✳✴✵✶✷✸✹✺✻✼✽✾✿❀❁❂❃❄❅❆❇❈❉❊❋❖★☆⁂⁎⁑"),
        new SymbolTab("sym.1", "❣❤❥❦♠♥♡❧♤☃☻☺☹ツ✌✍☚☛☜☝☞☟ღ"),
        new SymbolTab("sym.2", "➱➲➳➴➵➶➷➸➘➙➚➛➜➝➞➟➠➡➢➣➤➥➦➧➨➩➪➫➬➭➮➯➔➹➺➻➼➽➾←↑→↓↔↕↖↗↘↙↚↛↜↝↞↟↠↡↢↣↤↥↦↧↨↩↪↫↬↭↮↯↰↱↲↳↴↵↶↷↸↹↼↽↾↿⇀⇁⇂⇃⇄⇅⇆⇇⇈⇉⇊⇋⇌⇍⇎⇏⇐⇑⇒⇓⇔⇕⇖⇗⇘⇙⇚⇛⇜⇝⇞⇟⇠⇡⇢⇣⇤⇥⇦⇧⇨⇩⇪☇↺↻⇵⏏⏩⏪⏭⏮⏯⊲⊳⊴⊵⤴⤵⟵⟶⟷«»‹›"),
        new SymbolTab("sym.3", "⛏✂☂☔⛄☃⌛⌚☎☏✁✃✄✆✎✏✐✑✒✈⚓⚡⭐♔♕♚♛♨⚔☠⚠۞⛀⛁⛃⛂⚒⚑⚐⛨⚖⚗⚙⚛⚜⛓✉⏳⚱⚕⚚☤⚘☕⛺⛵⌂⛰⛩"),
        new SymbolTab("sym.4", "☁☀☄⛈❄☼☂☔☃⛄⛅☾☽❅❆⚡☘♣❀✿❁⚘❖"),
        new SymbolTab("sym.5", "⚀⚁⚂⚃⚄⚅♤♧♡♢♪♩♫♬♔♕♖♗♘♙♚♛♜♝♞♟♠♣♥♦♭♮♯"),
        new SymbolTab("sym.6", "╳✕✖✗✘☓❌♱♰✞✟†‡☨☒✓✔☑✙✚✛✜"),
        new SymbolTab("sym.7", "◆◇◈◊⋄♦♢▲△▴▵▼▽▾▿▶▷▸▹►▻◀◁◂◃◄◅◭◮"),
        new SymbolTab("sym.8", "◍◎●◐◑◒◓◔◕◖◗◯✇☮☯❍⊕⊖⊗⊘⊙⊚⊛⊜⊝○｡☣☢㊣⭘◦☪⏺∅⋅㊚㊛･∙⋮⋯⋰⋱°∵∴ﾟ❂∶∷•᛫᛬☉✚✪✣✤✥✦❉❃❁❀☖☗☀·‣⁃⦁⦾⦿⁌⁍❏❐❑❒❖➧⁕⁜"),
        new SymbolTab("sym.9", "▁▂▃▄▅▆▇█▉▊▋▌▍■▬▏▕▐░▒▓▔▀⧈□▣▤▥▦▧▨▩▪▫▭▮▯▰▱◘◙◚◛◧◨◩◪◫☐❏❐❑❒回♏♒▢◉◌◢◣◤◥"),
        new SymbolTab("sym.10", "─━│┃┄┅┆┇┈┉┊┋┌┍–—┱┲‑‒―﹏﹋﹌﹉﹊﹍﹎☰☱☳☴☶☷☲☵‾‗⁃Ξ┎┏┐┑┒┓└┕┖┗┘┙┚┛├┝┞┟┠┡┢┣┤┥┦┧┨┩┪┫┬┭┮┯┰┳┴┵┶┷┸┹┺┻┼┽┾┿╀╁╂╃╄╅╆╇╈╉╊╋╌╍╎╏═║╒╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡╢╣╤╥╦╧╨╩╪╫╬╭╮╯╰╱╲╴╵╶╷╸╹╺╻╼╽╾╿☽☾⌃⌄⌅⌆⌇⌈⌉⌊⌋﹃﹄「」‖︴⌠⌡ΣΠ«»๑⁅⁆⁐ℵℶℷℸ◠◡ぃ【】⁽⁾ʔʕ‸‹›╳▀▄█▌▐░▒▓¦⎯〖〗『』〔〕《》〈〉⟦⟧⟨⟩❨❩❪❫❬❭❮❯❰❱❲❳❴❵"),
        new SymbolTab("sym.11", "‘’‚‛“”„‟′″‴‵‶‷՚՛՜՝՞❛❜❝❞"),
        new SymbolTab("sym.12", "⚗⚑✉⚐☠☣☤☩☫☬☭♂♀☿⛨۩☊☋☌☍⁈⁉☥∡∢∱∲∳∸∞Δʊღ₪∀®©℠℡™✓✔✕✖✗✘☑☒⚠☢☮☯☪☸✚✛✜✝✞✟✠⚤⚥⚦⚧℗№¶†‡※☺☻☹"),
        new SymbolTab("sym.13", "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀᴛᴜᴠᴡʏᴢａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩꜰꜱ"),
        new SymbolTab("sym.14", "⓪①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳❶❷❸❹❺❻❼❽❾❿⓫⓬⓭⓮⓯⓰⓱⓲⓳⓴➊➋➌➍➎➏➐➑➒➓⑴⑵⑶⑷⑸⑹⑺⑻⑼⑽⑾⑿⒀⒁⒂⒃⒄⒅⒆⒇⒈⒉⒊⒋⒌⒍⒎⒏⒐⒑⒒⒓⒔⒕⒖⒗⒘⒙⒚⒛ⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩⅪⅫⅬ⁰⁴⁵⁶⁷⁸⁹₀₁₂₃₄₅₆₇₈₉➀➁➂➃➄➅➆➇➈➉¹²³½⅓⅔¼¾"),
        new SymbolTab("sym.15", "½⅓¼⅕⅙⅐⅛⅑⅒⅔⅖¾⅗⅜⅘⅚⅝⅞℅⅟‱⁺⁻⁼⁽⁾ⁿⁱ₊₋₌₍₎ₐₑₒₓₔ≤≥≦≧≨≩≮≯≰≱≲≳≴≵≡±−≣≪≫∹∺∻∼∽∾∿≀≁≂≃≄≅≆≇≈≉≊≋≌≍⊢⊣⊤⊥⊦⊧⊨⊩⊪⊫⊬⊭⊮⊯≎≏≐≑≒≓≔≕≖≗≘≙≚≛≜≝≞≟≠≢≬≭≶≷≸≹≺≻≼≽≾≿⊀⊁⊂⊃⊄⊅⊆⊇⊈⊉⊊⊋⊌⊍⊎⊏⊐⊑⊒⊓⊔⊡⊰⊱⊲⊳⊴⊵⊶⊷⊸⊹⊺⊻⊼⊽⊾⊿⋀⋁⋂⋃⋇⋈⋉⋊⋋⋌⋍⋎⋏⋐⋑⋒⋓⋔⋕⋖⋗⋘⋙⋚⋛⋜⋝⋞⋟∟∃∄∉∋∌∧∨∥⋠⋡⋢⋣⋤⋥⋦⋧⋨⋩⋪⋫⋬⋭⋲⋳⋴⋵⋶⋷⋸⋹⋺⋻⋼⋽⋾⋿∣∤∦∩∪∫∬∭∮∯∰∊∍∈×÷∑∏√∛∆∇∂¬∀∅πΩµ°‰′″€£¥₽₴₿¢"),
        new SymbolTab("sym.16", "ʂʐɶǍǎǞǟǺǻȂȃȦȧǠǡḀḁȀȁḆḇḄḅᵬḈḉḐḑḒḓḎḏḌᵭḔḕḖḗḘḙḜḝȨȩḚḛȄȅȆᵮǴǵǦḦḧḨḩḪḫȞȟḤẖḮḯȊȋǏǐȈȉḬḭǰȷǨǩḲḳḴḵḺḻḼḽḶḷḸḹⱢḾḿṂṃᵯṄṅṆṇṊṋǸǹṈṉᵰǬǭȬȭṌṍṎṏṐṑṒṓȎȏȪȫǑǒȮȯȰȱȌȍǪṔṕᵱȒȓṘṙṜṝṞṟȐȑṚᵳᵲṤṥṦṧṢṣṨṩᵴṰṱṮṯṬẗᵵṲṳṶṷṸṹṺṻǓǔǕǖǗǘǙǚǛǜṴṵȔȕȖṾṿṼṽẆẇẈẉẘẌẍẊẋȲȳẎẏẙẔẕẐẑẓᵶǮǯẛꜾꜿǢǣᵺỻᴂᴔȸʣʥʤʩʪʫȹʨʦʧỺƀƂƃƇƈƊƋƌƓǤǥƗƖɩƘƙƝƤƥɽƦƬƭƫƮȗƱƜƳƴƵƶƢƣȢȣʭʮʯƍƺⱾȿⱿɀᶀꟄꞔᶁᶂᶃꞕᶄᶅᶆᶇᶈᶉᶊᶋᶌᶍᶎᶏᶐᶒᶓᶔᶕᶖᶗᶘᶙᶚẚÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖÙÚÛÜÝàáâãäåæçìíîïñòóôõöùúûüýÿĀāĂăĄąĆćĈĉĊċČčĎďĐđĒēĔĕĖėĘęĚěĜĝḠḡĞğĠġĢģĤĥĦħĨĩĪīĬĭĮįİıĴĵĶķĹĺĻļĽľĿŀŁłŃńŅņŇňŊŋŌōŎŏŐőŒœŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŧŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžǼǽǾǿȘșȚțḂḃḊḋḞḟḢḣḰḱṀṁṖṗṠṡṪṫẀẁẂẃẄẅỲỳèéêëŉǧǫḍḥṛṭẒỊịỌọỤụȇƔɣʃẴẵẼẽỄễỒỠỡỮỸỹǱǲǳǄǅǆǇǈǊǋǌᵫꜲꜳꜴꜵꜶꜷꜸꜺꜼꜽꝎꝏꝠꝡƠơƯưẮắẤấẾếốỚớỨứẰằẦầỀềồỜờỪừẢảẲẳẨẩẺẻổỞỂểỈỉỎỏỔởỦủỬửỶỷẠạẶặẬậẸẹỆệỘộỢợỰựỴỵỐƕẪẫỖỗữɼƄƅẟȽƚƛȠƞƟƧƨƪƸƹƻƼƽƾȡȴȵȶȺⱥȻȼɆɇȾⱦɁɂɃɄɈɉɊɋɌɍɎɏẜẝỼỽỾỿꞨꞩɱɳɲʈɖɡʡɕʑɸʝʢɻʁɦʋɰɬɮʘǀǃǂǁɓɗᶑʄɠʛɧɫɨʉʊɘɵɤɜɞɑɒɚɝƁƉƑƩƲꜧꜦɺⱱʠʗʖɭɷɿʅʆʓʚ¿×ØÞðøþⱯƆƎꞰꞀꝹᴚɅɐɔǝɟᵷɥᴉɾʞꞁɯɹʇʌʍʎə"),
        new SymbolTab("sym.17", "ԂԪԬԄԐԆԞԚԮԒԠԈԔԨԢԊԤԖԌԎԦԘԜԃԫԭԅԑԇԟԛԯԓԡԉԕԩԣԋԥԗԍԏԧԙԝϓϔΆΈΉΊΌΎΏΐΪΫάέήίΰϊϋόύώһΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩαβγδεζηθικλμνξοπρςστυφχψωЂЅІЈЉЊЋѕіјљњҚқҒғҰұӘәҖҗҢңҺԀ"),
        new SymbolTab("sym.18", "ꭣꭐꭑ₧אַאָﬔﬕﬗﬖﬓꚂꚀꚈꚄꚐꚊꚌꚔꚎꚒꚖꚆꚃꚁꚉꚅꚑꚋꚍꚕꚏꚓꚗꚇἈἀἉἁἊἂἋἃἌἄἍἅἎἆἏἇᾺὰᾸᾰᾹᾱΆάᾈᾀᾉᾁᾊᾂᾋᾃᾌᾄᾍᾅᾎᾆᾏᾇᾼᾴᾶᾷᾲᾳἘἐἙἑἚἒἛἓἜἔἝἕῈΈὲέἨἠῊὴἩἡἪἢἫἣἬἤἭἥἮἦἯἧᾘᾐᾙᾑᾚᾒᾛᾓᾜᾔᾝᾕᾞᾖᾟᾗΉήῌῃῂῄῆῇῚὶΊίἸἰἹἱἺἲἻἳἼἴἽἵἾἶἿἷῘῐῙῑῒΐῖῗῸὸΌόὈὀὉὁὊὂὋὃὌὄὍὅῬῤῥῪὺΎύὙὑὛὓὝὕὟὗῨῠῩῡῢΰῧὐὒὔῦὖῺὼΏώὨὠὩὡὪὢὫὣὬὤὭὥὮὦὯὧᾨᾠᾩᾡᾪᾢᾫᾣᾬᾤᾭᾥᾮᾦᾯᾧῼῳῲῴῶῷ₿№ℹﬄﬆᚡᚵႠႡႢႣႤႥႦႧႨႩႪႫႬႭႮႯႰႱႲႳႴႵႶႷႸႹႺႻႼႽႾႿჀჁჂჃჄჅჇჍაბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰჱჲჳჴჵჶჷჸჹჺ჻ჼჽჾჿתּשׂפֿפּכּײַיִוֹוּבֿבּ₪₾֊ⴀⴁⴂⴃⴄⴅⴆⴡⴇⴈⴉⴊⴋⴌⴢⴍⴎⴏⴐⴑⴒⴣⴓⴔⴕⴖⴗⴘⴙⴚⴛⴜⴝⴞⴤⴟⴠⴥ₴≠אבגדהוזחטיכלמםנןסעפףצץקרᗺᗡℲ⅁⟘∩⅄ԱԲԳԴԶԷԹԺԻԼԽԾԿՀՁՂՃՄՅՆՇՈՉՋՌՍՎՏՐՑՒՓՔՕՖՙաբգդեզէըթժիլխծկհձղճմյնշոչպջռսվտրցւփքօֆևשתԸ∞")
    );
}

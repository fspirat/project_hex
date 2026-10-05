package ru.fspirat.hexgen;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Градиент со скриншота — тот же разбор, что на сайте (hex_generator, «Узнать градиент по скриншоту»):
 * пиксели текста Minecraft узнаются по тени (в 4 раза темнее, со сдвигом на пиксель шрифта вниз-вправо),
 * связные области — буквы, полосы — строки, большие промежутки — пробелы; цвета градиента подбираются
 * методом наименьших квадратов по той же формуле, по которой генератор красит текст.
 * Без зависимостей от игры — только JDK.
 */
public final class ShotGradient {
    private ShotGradient() {}

    /** chars — цвет каждого символа строки (−1 — пробел); stops — подобранные цвета; alt — без повторов, если они есть. */
    public record Line(int y, int x0, int x1, int[] chars, List<String> stops, double err, List<String> alt, double altErr) {
        public int glyphs() {
            int n = 0;
            for (int c : chars) if (c >= 0) n++;
            return n;
        }
    }

    public record Result(List<Line> lines, boolean exact, int unit) {}

    private record Comp(int id, int x0, int y0, int x1, int y1, int n, int seed) {}

    private static final class Glyph {
        int x0, x1, y0, y1;
        List<Integer> ids = new ArrayList<>();
        int[] rgb;
    }

    private static int v(int c) {
        return Math.max((c >> 16) & 255, Math.max((c >> 8) & 255, c & 255));
    }

    private static boolean shadowOk(int a, int b, boolean strict) {
        int va = v(a);
        if (va < 40) return false;
        int tol = strict ? 1 : 4 + (va >> 5);
        for (int s = 0; s <= 16; s += 8) {
            if (Math.abs(((b >> s) & 255) - (((a >> s) & 255) >> 2)) > tol) return false;
        }
        return va - v(b) >= 24;
    }

    private static double median(double[] a, int n) {
        if (n == 0) return 0;
        double[] s = Arrays.copyOf(a, n);
        Arrays.sort(s);
        return s[n >> 1];
    }

    public static Result analyze(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight(), N = w * h;
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        int[] V = new int[N];
        for (int i = 0; i < N; i++) V[i] = v(px[i]);

        // размер «пикселя шрифта» — сдвиг, при котором у ярких пикселей чаще всего находится тень
        int u = 0, best = 0, step = N > 2_000_000 ? 2 : 1;
        for (int s = 1; s <= 12; s++) {
            int cnt = 0;
            for (int y = 0; y + s < h; y += step) for (int x = 0; x + s < w; x += step) {
                int p = y * w + x;
                if (V[p] >= 40 && shadowOk(px[p], px[p + s * w + s], false)) cnt++;
            }
            if (cnt > best * 1.15) { best = cnt; u = s; }
        }
        if (best * step * step < 25) return new Result(List.of(), false, 0);
        int strict = 0;
        for (int y = 0; y + u < h; y += step) for (int x = 0; x + u < w; x += step) {
            int p = y * w + x;
            if (V[p] >= 40 && shadowOk(px[p], px[p + u * w + u], true)) strict++;
        }
        boolean exact = strict >= best * 0.6;

        // маска текста: пиксели с тенью + соседи того же цвета (их тень закрыта соседними пикселями буквы)
        byte[] M = new byte[N];
        int[] stack = new int[N];
        int sp = 0;
        for (int y = 0; y + u < h; y++) for (int x = 0; x + u < w; x++) {
            int p = y * w + x;
            if (V[p] < 40) continue;
            if (x >= u && y >= u && shadowOk(px[p - u * w - u], px[p], false)) continue;   // это сама тень
            if (shadowOk(px[p], px[p + u * w + u], false)) { M[p] = 2; stack[sp++] = p; }
        }
        while (sp > 0) {
            int p = stack[--sp], x = p % w, y = p / w, c = px[p];
            for (int dy = -1; dy <= 1; dy++) {
                int yy = y + dy;
                if (yy < 0 || yy >= h) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    int xx = x + dx;
                    if (xx < 0 || xx >= w) continue;
                    int o = yy * w + xx;
                    if (M[o] != 0 || V[o] < 40) continue;
                    int d = px[o];
                    if (Math.abs(((d >> 16) & 255) - ((c >> 16) & 255)) <= 10 && Math.abs(((d >> 8) & 255) - ((c >> 8) & 255)) <= 10
                            && Math.abs((d & 255) - (c & 255)) <= 10) { M[o] = 1; stack[sp++] = o; }
                }
            }
        }

        // связные области (8 соседей)
        int[] lab = new int[N];
        Arrays.fill(lab, -1);
        List<Comp> comps = new ArrayList<>();
        for (int p0 = 0; p0 < N; p0++) {
            if (M[p0] == 0 || lab[p0] >= 0) continue;
            int id = comps.size(), x0 = w, y0 = h, x1 = -1, y1 = -1, n = 0, seed = 0;
            sp = 0;
            stack[sp++] = p0;
            lab[p0] = id;
            while (sp > 0) {
                int q = stack[--sp], qx = q % w, qy = q / w;
                n++;
                if (M[q] == 2) seed++;
                x0 = Math.min(x0, qx); x1 = Math.max(x1, qx); y0 = Math.min(y0, qy); y1 = Math.max(y1, qy);
                for (int dy = -1; dy <= 1; dy++) {
                    int yy = qy + dy;
                    if (yy < 0 || yy >= h) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        int xx = qx + dx;
                        if (xx < 0 || xx >= w) continue;
                        int o = yy * w + xx;
                        if (lab[o] < 0 && M[o] != 0) { lab[o] = id; stack[sp++] = o; }
                    }
                }
            }
            comps.add(new Comp(id, x0, y0, x1, y1, n, seed));
        }
        double[] hs = new double[comps.size()];
        int hn = 0;
        for (Comp c : comps) if (c.n >= 3) hs[hn++] = c.y1 - c.y0 + 1;
        double mh = Math.max(1, median(hs, hn));
        List<Comp> keep = new ArrayList<>();
        for (Comp c : comps) {
            int cw = c.x1 - c.x0 + 1, ch = c.y1 - c.y0 + 1;
            if (c.n < Math.max(2, u * u)) continue;
            if (c.seed < c.n * 0.2) continue;
            if (cw >= 8 * ch && cw > mh * 3) continue;
            if (ch > mh * 2.6) continue;
            keep.add(c);
        }

        // строки — полосы по вертикали
        int[] rows = new int[h];
        for (Comp c : keep) for (int y = c.y0; y <= c.y1; y++) rows[y]++;
        List<Line> lines = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            if (rows[y] == 0) continue;
            int b0 = y, b1 = y;
            while (b1 + 1 < h && rows[b1 + 1] > 0) b1++;
            y = b1;
            if (b1 - b0 + 1 < 4) continue;
            final int fb0 = b0, fb1 = b1;
            List<Comp> cs = new ArrayList<>(keep.stream().filter(c -> c.y0 >= fb0 && c.y1 <= fb1).toList());
            cs.sort((a, b) -> Integer.compare(a.x0, b.x0));
            List<Glyph> gl = new ArrayList<>();
            for (Comp c : cs) {
                Glyph g = gl.isEmpty() ? null : gl.get(gl.size() - 1);
                if (g != null && c.x0 <= g.x1) {
                    g.x1 = Math.max(g.x1, c.x1); g.y0 = Math.min(g.y0, c.y0); g.y1 = Math.max(g.y1, c.y1); g.ids.add(c.id);
                } else {
                    Glyph ng = new Glyph();
                    ng.x0 = c.x0; ng.x1 = c.x1; ng.y0 = c.y0; ng.y1 = c.y1; ng.ids.add(c.id);
                    gl.add(ng);
                }
            }
            if (gl.isEmpty()) continue;
            for (Glyph g : gl) g.rgb = colorOf(g, g.x0, g.x1, w, px, V, lab);
            double[] ws = new double[gl.size()];
            for (int i = 0; i < gl.size(); i++) ws[i] = gl.get(i).x1 - gl.get(i).x0 + 1;
            double mw = Math.max(1, median(ws, ws.length));

            List<Glyph> gs = new ArrayList<>();
            for (Glyph g : gl) {
                // слипшиеся буквы: у соседних букв градиента разный цвет — делим по смене цвета
                if (exact && g.x1 - g.x0 + 1 > mw * 1.4) {
                    int start = g.x0;
                    int[] prev = null;
                    for (int x = g.x0; x <= g.x1; x++) {
                        int[] c = colorOf(g, x, x, w, px, V, lab);
                        if (c == null) continue;
                        if (prev != null && diff(c, prev) >= 2 && x - start >= u * 2) {
                            gs.add(part(g, start, x - 1, w, px, V, lab));
                            start = x;
                        }
                        prev = c;
                    }
                    gs.add(part(g, start, g.x1, w, px, V, lab));
                } else gs.add(g);
            }
            // части одной буквы («ы», «"»): узкий кусок того же цвета вплотную к соседу
            List<Glyph> merged = new ArrayList<>();
            for (Glyph g : gs) {
                if (g.rgb == null) continue;
                Glyph m = merged.isEmpty() ? null : merged.get(merged.size() - 1);
                if (m != null && diff(m.rgb, g.rgb) <= 1 && g.x0 - m.x1 - 1 <= u
                        && Math.min(g.x1 - g.x0, m.x1 - m.x0) + 1 <= mw * 0.4) {
                    m.x1 = g.x1; m.ids.addAll(g.ids);
                } else merged.add(g);
            }
            if (merged.isEmpty()) continue;

            // пробелы: между буквами 1 пиксель шрифта, пробел добавляет ещё ~4; очень большой разрыв — уже другая строка
            List<List<Glyph>> parts = new ArrayList<>();
            List<Glyph> cur = new ArrayList<>();
            cur.add(merged.get(0));
            parts.add(cur);
            for (int i = 1; i < merged.size(); i++) {
                int gap = merged.get(i).x0 - merged.get(i - 1).x1 - 1;
                if (gap > u * 14) { cur = new ArrayList<>(); parts.add(cur); cur.add(merged.get(i)); continue; }
                if (gap > u * 2.6) for (int s = Math.max(1, (int) Math.round((gap - u) / (4.0 * u))); s > 0; s--) cur.add(null);
                cur.add(merged.get(i));
            }
            for (List<Glyph> chars : parts) {
                List<Glyph> real = chars.stream().filter(java.util.Objects::nonNull).toList();
                if (real.size() < 2 || chars.size() - real.size() > real.size()) continue;
                int ly0 = real.stream().mapToInt(g -> g.y0).min().orElse(0), ly1 = real.stream().mapToInt(g -> g.y1).max().orElse(0);
                if (ly1 - ly0 + 1 < u * 3) continue;
                int[] colors = new int[chars.size()];
                for (int i = 0; i < chars.size(); i++) colors[i] = chars.get(i) == null ? -1 : rgbInt(chars.get(i).rgb);
                Line line = fit(ly0, real.get(0).x0, real.get(real.size() - 1).x1, colors);
                double[] vs = new double[real.size()];
                for (int i = 0; i < real.size(); i++) vs[i] = Math.max(real.get(i).rgb[0], Math.max(real.get(i).rgb[1], real.get(i).rgb[2]));
                if (line.err() > 6 && median(vs, vs.length) < 110) continue;   // тёмный шум с фона
                lines.add(line);
            }
        }
        return new Result(lines, exact, u);
    }

    private static Glyph part(Glyph g, int x0, int x1, int w, int[] px, int[] V, int[] lab) {
        Glyph n = new Glyph();
        n.x0 = x0; n.x1 = x1; n.y0 = g.y0; n.y1 = g.y1; n.ids = new ArrayList<>(g.ids);
        n.rgb = colorOf(n, x0, x1, w, px, V, lab);
        return n;
    }

    private static int diff(int[] a, int[] b) {
        return Math.max(Math.abs(a[0] - b[0]), Math.max(Math.abs(a[1] - b[1]), Math.abs(a[2] - b[2])));
    }

    private static int rgbInt(int[] c) {
        return (c[0] << 16) | (c[1] << 8) | c[2];
    }

    /** Цвет участка буквы: медиана самых ярких пикселей. */
    private static int[] colorOf(Glyph g, int xa, int xb, int w, int[] px, int[] V, int[] lab) {
        Set<Integer> ids = new HashSet<>(g.ids);
        int cap = (xb - xa + 1) * (g.y1 - g.y0 + 1);
        double[] r = new double[cap], gr = new double[cap], b = new double[cap], vs = new double[cap];
        int n = 0;
        for (int y = g.y0; y <= g.y1; y++) for (int x = xa; x <= xb; x++) {
            int p = y * w + x;
            if (!ids.contains(lab[p])) continue;
            int c = px[p];
            r[n] = (c >> 16) & 255; gr[n] = (c >> 8) & 255; b[n] = c & 255; vs[n] = V[p];
            n++;
        }
        if (n == 0) return null;
        double cut = median(vs, n) * 0.85;
        double[] rr = new double[n], gg = new double[n], bb = new double[n];
        int m = 0;
        for (int i = 0; i < n; i++) if (vs[i] >= cut) { rr[m] = r[i]; gg[m] = gr[i]; bb[m] = b[i]; m++; }
        return new int[]{(int) Math.round(median(rr, m)), (int) Math.round(median(gg, m)), (int) Math.round(median(bb, m))};
    }

    /** Вес каждого цвета градиента для символа j из n — формула генератора (HexCore.gradientGlyphs). */
    private static double[] basis(int n, int k, int j) {
        double[] wt = new double[k];
        if (k == 1) { wt[0] = 1; return wt; }
        for (int p = 0; p < k - 1; p++) {
            int a = (int) Math.round(n * (double) p / (k - 1)), b = (int) Math.round(n * (double) (p + 1) / (k - 1));
            if (j >= a && j < b) {
                int m = b - a;
                double t = m > 1 ? (j - a) / (double) (m - 1) : 0;
                wt[p] = 1 - t;
                wt[p + 1] += t;
                return wt;
            }
        }
        wt[k - 1] = 1;
        return wt;
    }

    private static int[][] solve(List<double[]> rows, List<int[]> ys, int k) {
        double[][] A = new double[k][k + 3];
        for (int r = 0; r < rows.size(); r++) {
            double[] w = rows.get(r);
            for (int i = 0; i < k; i++) {
                if (w[i] == 0) continue;
                for (int j = 0; j < k; j++) A[i][j] += w[i] * w[j];
                for (int c = 0; c < 3; c++) A[i][k + c] += w[i] * ys.get(r)[c];
            }
        }
        for (int i = 0; i < k; i++) A[i][i] += 1e-6;
        for (int i = 0; i < k; i++) {
            int p = i;
            for (int r = i + 1; r < k; r++) if (Math.abs(A[r][i]) > Math.abs(A[p][i])) p = r;
            double[] tmp = A[i]; A[i] = A[p]; A[p] = tmp;
            for (int r = 0; r < k; r++) {
                if (r == i) continue;
                double f = A[r][i] / A[i][i];
                if (f != 0) for (int c = i; c < k + 3; c++) A[r][c] -= f * A[i][c];
            }
        }
        int[][] st = new int[k][3];
        for (int i = 0; i < k; i++) for (int c = 0; c < 3; c++) st[i][c] = (int) Math.max(0, Math.min(255, Math.round(A[i][k + c] / A[i][i])));
        return st;
    }

    private static String hex(int[] c) {
        return String.format("%02X%02X%02X", c[0], c[1], c[2]);
    }

    private static Line fit(int y, int x0, int x1, int[] chars) {
        int n = chars.length;
        List<Integer> known = new ArrayList<>();
        List<int[]> ys = new ArrayList<>();
        for (int j = 0; j < n; j++) if (chars[j] >= 0) { known.add(j); ys.add(new int[]{(chars[j] >> 16) & 255, (chars[j] >> 8) & 255, chars[j] & 255}); }
        int K = Math.max(1, Math.min(6, n / 2 + 1));
        List<List<String>> stops = new ArrayList<>();
        double[] errs = new double[K];
        for (int k = 1; k <= K; k++) {
            List<double[]> rows = new ArrayList<>();
            for (int j : known) rows.add(basis(n, k, j));
            int[][] st = solve(rows, ys, k);
            double e = 0;
            for (int r = 0; r < rows.size(); r++) for (int c = 0; c < 3; c++) {
                double v = 0;
                for (int i = 0; i < k; i++) v += rows.get(r)[i] * st[i][c];
                e += (v - ys.get(r)[c]) * (v - ys.get(r)[c]);
            }
            errs[k - 1] = Math.sqrt(e / Math.max(1, ys.size() * 3));
            List<String> hs = new ArrayList<>();
            for (int[] s : st) hs.add(hex(s));
            stops.add(hs);
        }
        double bestErr = Arrays.stream(errs).min().orElse(0);
        int pick = K - 1;
        for (int k = 0; k < K; k++) if (errs[k] <= bestErr + Math.max(2.5, bestErr * 0.35)) { pick = k; break; }
        List<String> chosen = stops.get(pick);
        // одинаковые цвета подряд: часть текста одного цвета — предлагаем и вариант без повторов
        List<String> uniq = new ArrayList<>();
        for (String s : chosen) {
            if (!uniq.isEmpty() && near(uniq.get(uniq.size() - 1), s)) continue;
            uniq.add(s);
        }
        List<String> alt = uniq.size() < chosen.size() && uniq.size() >= 2 ? stops.get(uniq.size() - 1) : null;
        return new Line(y, x0, x1, chars, chosen, errs[pick], alt, alt == null ? 0 : errs[uniq.size() - 1]);
    }

    private static boolean near(String a, String b) {
        int x = Integer.parseInt(a, 16), y = Integer.parseInt(b, 16);
        return Math.abs(((x >> 16) & 255) - ((y >> 16) & 255)) <= 4 && Math.abs(((x >> 8) & 255) - ((y >> 8) & 255)) <= 4
                && Math.abs((x & 255) - (y & 255)) <= 4;
    }
}

package com.yourorg.servershop.util;

import java.util.*;
import org.bukkit.Material;

public final class Fuzzy {
    private Fuzzy() {}

    public static String normalize(String s) {
        String t = s == null ? "" : s.trim().toUpperCase(Locale.ROOT).replace('_',' ');
        StringBuilder sb = new StringBuilder(t.length());
        for (int i=0;i<t.length();i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c) || c==' ') sb.append(c);
        }
        return sb.toString().replaceAll("\\s+"," ").trim();
    }

    public static double similarity(String a, String b) {
        String A = normalize(a), B = normalize(b);
        if (A.isEmpty() || B.isEmpty()) return 0.0;
        if (A.equals(B)) return 1.0;
        if (B.contains(A)) return Math.min(1.0, 0.7 + (A.length() / (double)B.length()) * 0.3);
        if (A.contains(B)) return Math.min(1.0, 0.7 + (B.length() / (double)A.length()) * 0.3);
        int d = levenshtein(A, B);
        double max = Math.max(A.length(), B.length());
        return Math.max(0.0, 1.0 - (d / max));
    }

    public static int levenshtein(String s1, String s2) {
        int n = s1.length(), m = s2.length();
        if (n==0) return m; if (m==0) return n;
        int[] prev = new int[m+1];
        int[] curr = new int[m+1];
        for (int j=0;j<=m;j++) prev[j]=j;
        for (int i=1;i<=n;i++) {
            curr[0]=i; char c1=s1.charAt(i-1);
            for (int j=1;j<=m;j++) {
                int cost = (c1==s2.charAt(j-1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j-1]+1, prev[j]+1), prev[j-1]+cost);
            }
            int[] tmp=prev; prev=curr; curr=tmp;
        }
        return prev[m];
    }

    public static java.util.List<Material> rankMaterials(java.util.Collection<Material> mats, String query, int limit, double threshold) {
        String q = normalize(query);
        java.util.List<MaterialScore> list = new java.util.ArrayList<>();
        for (Material m : mats) {
            String name = m.name();
            double s = similarity(q, name);
            if (s >= threshold) list.add(new MaterialScore(m, s));
        }
        list.sort((a,b)->{
            int c = Double.compare(b.s, a.s);
            return c!=0?c:a.m.name().compareTo(b.m.name());
        });
        java.util.List<Material> out = new java.util.ArrayList<>();
        for (int i=0;i<list.size() && out.size()<limit;i++) out.add(list.get(i).m);
        return out;
    }

    private static final class MaterialScore { final Material m; final double s; MaterialScore(Material m, double s){this.m=m;this.s=s;} }
}

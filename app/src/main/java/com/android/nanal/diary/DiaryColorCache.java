package com.android.nanal.diary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


public class DiaryColorCache implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final String SEPARATOR = "::";

    private Map<String, ArrayList<Integer>> mColorPaletteMap;
    private Map<String, String> mColorKeyMap;
//    private Map<String, Integer> mColorKeyMap;

    public DiaryColorCache() {
        mColorPaletteMap = new HashMap<String, ArrayList<Integer>>();
        mColorKeyMap = new HashMap<String, String>();
    }

    /**
     * Inserts a color into the cache.
     */
    public void insertColor(String userId, int displayColor, String colorKey) {
        // 개인 일기
        insertColor(userId, -1, displayColor, colorKey);
    }

    public void insertColor(String groupName, int groupId, int displayColor, String colorKey) {
        mColorKeyMap.put(createKey(groupName, groupId), colorKey);
        String key = createKey(groupName, groupId);
        ArrayList<Integer> colorPalette;
        if((colorPalette = mColorPaletteMap.get(key)) == null) {
            colorPalette = new ArrayList<Integer>();
        }
        colorPalette.add(displayColor);
        mColorPaletteMap.put(key, colorPalette);
    }
    /**
     * Retrieve an array of colors for a specific account name and type.
     */
    public int[] getColorArray(String groupName, int groupId) {
        ArrayList<Integer> colors = mColorPaletteMap.get(createKey(groupName, groupId));
        if (colors == null) {
            return null;
        }
        int[] ret = new int[colors.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = colors.get(i);
        }
        return ret;
    }

    /**
     * Retrieve an event color's unique key based on account name, type, and color.
     */
    public String getColorKey(String userId, int displayColor) {
        return getColorKey(userId, -1, displayColor);
    }
    public String getColorKey(String accountName, int groupId, int displayColor) {
        return mColorKeyMap.get(createKey(accountName, groupId, displayColor));
    }

    /**
     * Sorts the arrays of colors based on a comparator.
     */
    public void sortPalettes(Comparator<Integer> comparator) {
        for (String key : mColorPaletteMap.keySet()) {
            ArrayList<Integer> palette = mColorPaletteMap.get(key);
            Integer[] sortedColors = new Integer[palette.size()];
            Arrays.sort(palette.toArray(sortedColors), comparator);
            palette.clear();
            for (Integer color : sortedColors) {
                palette.add(color);
            }
            mColorPaletteMap.put(key, palette);
        }
    }

    private String createKey(String groupName, int groupId) {
        return new StringBuilder().append(groupName)
                .append(SEPARATOR)
                .append(groupId)
                .toString();
    }

    private String createKey(String accountName, int groupId, int displayColor) {
        return new StringBuilder(createKey(accountName, groupId))
                .append(SEPARATOR)
                .append(displayColor)
                .toString();
    }
}

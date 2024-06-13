package com.common_creation.applet.logger;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import uicc.toolkit.ProactiveHandler;
import uicc.toolkit.ProactiveHandlerSystem;

/**
 * 画面表示用のユーティリティクラス。
 */
public class Screen {

    private static final byte DCS_8_BIT_DATA = 0x04;
    private static byte[] displayString;

    /**
     * 指定されたテキストを指定されたオフセットと長さで表示します。
     *
     * @param text    表示するテキスト
     * @param offset  テキストのオフセット
     * @param length  テキストの長さ
     */
    public static void displayText(byte[] text, short offset, short length) {
        ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
        ph.initMoreTime();
        ph.initDisplayText((byte) 0x81, DCS_8_BIT_DATA, text, offset, length);
        ph.send();
    }

    /**
     * 指定されたテキストを最初から指定された長さまで表示します。
     *
     * @param text    表示するテキスト
     * @param length  テキストの長さ
     */
    public static void displayText(byte[] text, short length) {
        displayText(text, (short) 0, length);
    }

    /**
     * 指定されたテキスト全体を表示します。
     *
     * @param text    表示するテキスト
     */
    public static void displayText(byte[] text) {
        displayText(text, (short) text.length);
    }

    /**
     * エラーメッセージと理由を表示します。
     *
     * @param exceptionName エラーの名前
     * @param reason        エラーの理由コード
     */
    public static void displayError(byte[] exceptionName, short reason) {
        if (displayString == null) {
            displayString = JCSystem.makeTransientByteArray((short) 80, JCSystem.CLEAR_ON_RESET);
        }
        if (displayString == null) {
            return;
        }

        short length = (short) exceptionName.length;
        if (length > (short) (displayString.length - 8)) {
            length = (short) (displayString.length - 8);
        }

        short i = Util.arrayCopyNonAtomic(exceptionName, (short) 0, displayString, (short) 0, length);
        displayString[i++] = (byte) ':';

        Util.setShort(displayString, (short) (i + 4), reason);  // Reasonをセット
        i = HexUtil.bytesToHex(displayString, (short) (i + 4), (short) 2, displayString, i);

        displayText(displayString, (short) 0, i);
    }
}

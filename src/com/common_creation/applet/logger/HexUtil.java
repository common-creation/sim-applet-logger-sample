package com.common_creation.applet.logger;

/**
 * HEX操作のためのユーティリティクラス。
 */
public class HexUtil {

    private static final byte[] HEX_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * バイト配列を16進数表現のバイト配列に変換します。
     *
     * @param inBuffer  変換対象のバイト配列
     * @param inOffset  変換を開始するオフセット
     * @param inLength  変換するバイト数
     * @param outBuffer 結果を格納するバイト配列
     * @param outOffset 結果を格納するバイト配列の開始位置
     * @return 結果バイト配列の次の書き込み位置
     */
    public static short bytesToHex(byte[] inBuffer, short inOffset, short inLength, byte[] outBuffer, short outOffset) {
        short j = outOffset;
        short endOffset = (short) (inOffset + inLength);
        for (short i = inOffset; i < endOffset; i++) {
            outBuffer[j++] = HEX_ARRAY[(inBuffer[i] >> 4) & 0x0F];
            outBuffer[j++] = HEX_ARRAY[inBuffer[i] & 0x0F];
        }
        return j;
    }

    /**
     * short値を文字列形式のバイト配列に変換します。
     *
     * @param num             変換対象のshort値
     * @param outBuffer       結果を格納するバイト配列
     * @param outBufferOffset 結果を格納するバイト配列の開始位置
     * @return 結果バイト配列の次の書き込み位置
     */
    public static short numToCharArray(short num, byte[] outBuffer, short outBufferOffset) {
        if (num == 0) {
            outBuffer[outBufferOffset] = '0';
            return (short) (outBufferOffset + 1);
        }

        short digit = 0;
        boolean isNegative = false;
        if (num < 0) {
            num = (short) -num;
            digit++;
            isNegative = true;
        }

        short target = num;

        while (target > 0) {
            digit++;
            target /= 10;
        }

        short index = 0;
        short base = (short) (outBufferOffset + digit - 1);
        if (isNegative) {
            outBuffer[outBufferOffset] = (byte) '-';
            index++;
        }
        while (num > 0) {
            outBuffer[(short) (base - index)] = (byte) ((num % 10) + '0');
            num /= 10;
            index++;
        }

        return (short) (outBufferOffset + digit);
    }
}

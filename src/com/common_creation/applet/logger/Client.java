package com.common_creation.applet.logger;

import javacard.framework.APDUException;
import javacard.framework.JCSystem;
import javacard.framework.SystemException;
import javacard.framework.Util;
import uicc.access.UICCException;
import uicc.toolkit.*;

/**
 * HTTP通信を行うクライアントクラス。
 */
public class Client {

    private static final byte[] GET_HEADER = {'G', 'E', 'T', ' '};
    private static final byte[] POST_HEADER = {'P', 'O', 'S', 'T', ' '};
    private static final byte[] HTTP_VERSION_HEADER = {' ', 'H', 'T', 'T', 'P', '/', '1', '.', '1'};
    private static final byte[] HOST_HEADER = {'H', 'o', 's', 't', ':', ' '};
    private static final byte[] COLON = {':'};
    private static final byte[] CONNECTION_HEADER = {'C', 'o', 'n', 'n', 'e', 'c', 't', 'i', 'o', 'n', ':', ' ', 'c', 'l', 'o', 's', 'e'};
    private static final byte[] CONTENT_TYPE_HEADER = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'T', 'y', 'p', 'e', ':', ' ', 'a', 'p', 'p', 'l', 'i', 'c', 'a', 't', 'i', 'o', 'n', '/', 'o', 'c', 't', 'e', 't', '-', 's', 't', 'r', 'e', 'a', 'm'};
    private static final byte[] CONTENT_LENGTH_HEADER_PREFIX = {'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'L', 'e', 'n', 'g', 't', 'h', ':', ' '};
    private static final byte[] USER_AGENT_HEADER = {'U', 's', 'e', 'r', '-', 'A', 'g', 'e', 'n', 't', ':', ' ', 'A', 'p', 'p', 'l', 'e', 't', '/', '0', '.', '9'};
    private static final byte[] NEW_LINE_HEADER = {'\r', '\n'};
    private byte[] stateBuffer;
    private byte[] sendBuffer;

    private static final short SEND_CHANNEL_INDEX = 0;
    private static final short PROTOCOL_CHANNEL_INDEX = 1;
    private static final short SHOW_MESSAGE = 2;
    private static final short USE_BE_TAG = 3;
    private static final short RETRY_SEND_COUNT = 4;

    private static final byte FALSE = 0x00;
    private static final byte TRUE = 0x01;

    protected static final byte BUFFER_SIZE_TAG = (byte) 0xB9;
    private static final byte[] BE_TAG = {(byte) 0xBE, (byte) 0x00};
    private static final byte TCP_TAG = 0x02;

    /**
     * HttpClientのコンストラクタ。
     * トランジェントバイト配列を初期化し、状態バッファと送信バッファを設定します。
     */
    public Client() {
        stateBuffer = JCSystem.makeTransientByteArray((short) 6, JCSystem.CLEAR_ON_RESET);
        sendBuffer = JCSystem.makeTransientByteArray((short) 529, JCSystem.CLEAR_ON_RESET);

        stateBuffer[SHOW_MESSAGE] = FALSE;
        stateBuffer[USE_BE_TAG] = TRUE;
    }

    /**
     * POSTリクエストを送信します。
     * 指定されたアドレス、ポート、パス、およびボディを使用してHTTP POSTリクエストを構築し送信します。
     *
     * @param address    IPアドレスのバイト配列
     * @param domain     ドメイン名のバイト配列（nullの場合はIPアドレスを使用）
     * @param port       ポート番号
     * @param path       リクエストパスのバイト配列
     * @param pathLength リクエストパスの長さ
     * @param body       リクエストボディのバイト配列
     * @param bodyOffset リクエストボディのオフセット
     * @param bodyLength リクエストボディの長さ
     * @throws ToolkitException Toolkit APIの例外
     * @throws UICCException    UICC APIの例外
     * @throws APDUException    APDUプロトコルの例外
     */
    public void post(byte[] address, byte[] domain, short port, byte[] path, short pathLength, byte[] body, short bodyOffset, short bodyLength) throws ToolkitException, UICCException, APDUException {
        closeAllChannels();

        stateBuffer[RETRY_SEND_COUNT] = 0;

        if (stateBuffer[SHOW_MESSAGE] == TRUE) {
            Screen.displayText(POST_HEADER);
        }

        short headerLength = createHttpHeader(POST_HEADER, address, domain, port, path, pathLength, bodyLength);
        send(POST_HEADER, address, port, headerLength, body, bodyOffset, bodyLength);
    }

    /**
     * HTTPリクエストヘッダを作成します。
     * 指定されたHTTPメソッド、アドレス、ポート、パス、およびボディ長を使用してHTTPリクエストヘッダーを構築します。
     *
     * @param method     HTTPリクエストメソッド（GETまたはPOST）
     * @param address    IPアドレスのバイト配列
     * @param domain     ドメイン名のバイト配列（nullの場合はIPアドレスを使用）
     * @param port       ポート番号
     * @param path       リクエストパスのバイト配列
     * @param pathLength リクエストパスの長さ
     * @param bodyLength リクエストボディの長さ
     * @return HTTPリクエストヘッダの総長
     */
    private short createHttpHeader(byte[] method, byte[] address, byte[] domain, short port, byte[] path, short pathLength, short bodyLength) {

        short sendBufferOffset = 0;

        sendBufferOffset = Util.arrayCopy(method, (short) 0, sendBuffer, sendBufferOffset, (short) method.length);
        sendBufferOffset = Util.arrayCopy(path, (short) 0, sendBuffer, sendBufferOffset, pathLength);
        sendBufferOffset = Util.arrayCopy(HTTP_VERSION_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) HTTP_VERSION_HEADER.length);
        sendBufferOffset = Util.arrayCopy(NEW_LINE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) NEW_LINE_HEADER.length);
        sendBufferOffset = Util.arrayCopy(HOST_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) HOST_HEADER.length);

        if (domain == null) {
            for (short i = 0; i < (short) address.length; i++) {
                sendBufferOffset = HexUtil.numToCharArray((short) (address[i] & 0xFF), sendBuffer, sendBufferOffset);
                sendBuffer[sendBufferOffset++] = '.';
            }
            sendBufferOffset--;
        } else {
            sendBufferOffset = Util.arrayCopy(domain, (short) 0, sendBuffer, sendBufferOffset, (short) domain.length);
        }

        if (port != 80) {
            sendBufferOffset = Util.arrayCopy(COLON, (short) 0, sendBuffer, sendBufferOffset, (short) COLON.length);
            sendBufferOffset = HexUtil.numToCharArray(port, sendBuffer, sendBufferOffset);
        }

        sendBufferOffset = Util.arrayCopy(NEW_LINE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) NEW_LINE_HEADER.length);
        sendBufferOffset = Util.arrayCopy(CONNECTION_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) CONNECTION_HEADER.length);
        sendBufferOffset = Util.arrayCopy(NEW_LINE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) NEW_LINE_HEADER.length);
        sendBufferOffset = Util.arrayCopy(CONTENT_TYPE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) CONTENT_TYPE_HEADER.length);
        sendBufferOffset = Util.arrayCopy(NEW_LINE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) NEW_LINE_HEADER.length);

        if (bodyLength > 0) {
            sendBufferOffset = Util.arrayCopy(CONTENT_LENGTH_HEADER_PREFIX, (short) 0, sendBuffer, sendBufferOffset, (short) CONTENT_LENGTH_HEADER_PREFIX.length);
            sendBufferOffset = HexUtil.numToCharArray(bodyLength, sendBuffer, sendBufferOffset);
            sendBufferOffset = Util.arrayCopy(NEW_LINE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) NEW_LINE_HEADER.length);
        }

        sendBufferOffset = Util.arrayCopy(USER_AGENT_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) USER_AGENT_HEADER.length);
        sendBufferOffset = Util.arrayCopy(NEW_LINE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) NEW_LINE_HEADER.length);
        sendBufferOffset = Util.arrayCopy(NEW_LINE_HEADER, (short) 0, sendBuffer, sendBufferOffset, (short) NEW_LINE_HEADER.length);

        return sendBufferOffset;
    }

    /**
     * HTTPリクエストを送信します。
     * 指定されたメソッド、アドレス、ポート、ヘッダ長、ボディ、およびボディ長を使用してデータを送信します。
     *
     * @param method       HTTPリクエストメソッドのバイト配列（GETまたはPOST）
     * @param address      IPアドレスのバイト配列
     * @param port         ポート番号
     * @param headerLength ヘッダの長さ
     * @param body         リクエストボディのバイト配列
     * @param bodyOffset   リクエストボディのオフセット
     * @param bodyLength   リクエストボディの長さ
     * @throws ToolkitException Toolkit APIの例外
     * @throws UICCException    UICC APIの例外
     * @throws APDUException    APDUプロトコルの例外
     */
    private void send(byte[] method, byte[] address, short port, short headerLength, byte[] body, short bodyOffset, short bodyLength) throws ToolkitException, UICCException, APDUException {
        openChannel(address, port);

        stateBuffer[PROTOCOL_CHANNEL_INDEX] = (byte)(ToolkitConstants.DEV_ID_CHANNEL_BASE + this.getBIPChannelID());
        byte result = sendData(sendBuffer, (short) 0, headerLength);
        if (method != GET_HEADER) {
            result = sendData(body, bodyOffset, bodyLength);
        }

        if (result == ToolkitConstants.RES_CMD_PERF) {
            if (stateBuffer[SHOW_MESSAGE] == TRUE) {
                Screen.displayText(debugOk);
            }
        } else {
            closeChannel();
            APDUException.throwIt((short) result);
        }
    }

    private static final byte[] debugOk = {'d', 'e', 'b', 'u', 'g', ':', ' ', 'O', 'K'};

    /**
     * データを送信します。
     * 指定されたバッファのデータを分割して送信します。
     *
     * @param buffer 送信するデータのバイト配列
     * @param offset データのオフセット
     * @param length データの長さ
     * @return プロアクティブハンドラの送信結果
     * @throws ToolkitException Toolkit APIの例外
     */
    private byte sendData(byte[] buffer, short offset, short length) throws ToolkitException {
        ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();

        byte result = (byte) 0xFF;
        short position = offset;
        while (position < length) {
            ph.init(ToolkitConstants.PRO_CMD_SEND_DATA, (byte) 0x01, stateBuffer[PROTOCOL_CHANNEL_INDEX]);

            short remain = (short) (length - position);
            short append;
            if (remain > 0xA0) {
                ph.appendTLV(ToolkitConstants.TAG_CHANNEL_DATA, buffer, position, (short) 0xA0);
                append = 0xA0;
            } else {
                ph.appendTLV(ToolkitConstants.TAG_CHANNEL_DATA, buffer, position, remain);
                append = remain;
            }
            result = ph.send();
            if (result == ToolkitConstants.RES_CMD_PERF) {
                position += append;
            } else {
                break;
            }

            ph.initMoreTime();
            ph.send();
        }

        return result;
    }

    /**
     * TCPでチャネルをオープンします。
     * 指定されたアドレスとポートを使用してTCPチャネルを開きます。
     *
     * @param address IPアドレスのバイト配列
     * @param port    ポート番号
     * @throws ToolkitException Toolkit APIの例外
     * @throws UICCException    UICC APIの例外
     * @throws APDUException    APDUプロトコルの例外
     */
    protected void openChannel(byte[] address, short port) throws ToolkitException, UICCException, APDUException {
        ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
        ProactiveResponseHandler rh = ProactiveResponseHandlerSystem.getTheHandler();

        if (stateBuffer[SEND_CHANNEL_INDEX] != 0) {
            closeChannel();
        }

        ph.init(ToolkitConstants.PRO_CMD_OPEN_CHANNEL, (byte) 0x03, ToolkitConstants.DEV_ID_TERMINAL);
        ph.appendTLV((byte) (ToolkitConstants.TAG_BEARER_DESCRIPTION | ToolkitConstants.TAG_SET_CR), (byte) 0x03);
        ph.appendTLV((byte) (BUFFER_SIZE_TAG | ToolkitConstants.TAG_SET_CR), (short) 0x05DC);
        if (stateBuffer[USE_BE_TAG] == TRUE) {
            ph.appendArray(BE_TAG, (short) 0, (short) 2);
        }
        ph.appendTLV((byte) (ToolkitConstants.TAG_UICC_TERMINAL_TRANSPORT_LEVEL | ToolkitConstants.TAG_SET_CR), TCP_TAG , port);
        ph.appendTLV((byte) (ToolkitConstants.TAG_OTHER_DATA_DESTINATION_ADDRESS | ToolkitConstants.TAG_SET_CR), (byte) 0x21, address, (short) 0, (short) 4);

        byte openResult = ph.send();
        if (openResult == ToolkitConstants.RES_CMD_PERF) {
            stateBuffer[SEND_CHANNEL_INDEX] = rh.getChannelIdentifier();
        } else if (stateBuffer[RETRY_SEND_COUNT] < 3) {
            ph.initMoreTime();
            ph.send();

            stateBuffer[RETRY_SEND_COUNT]++;
            stateBuffer[USE_BE_TAG] = stateBuffer[USE_BE_TAG] == TRUE ? FALSE : TRUE;

            openChannel(address, port);
        } else {
            APDUException.throwIt((short) openResult);
        }
    }

    /**
     * BIPチャネルIDを取得します。
     *
     * @return BIPチャネルID
     */
    protected byte getBIPChannelID() {
        return stateBuffer[SEND_CHANNEL_INDEX];
    }

    /**
     * チャネルを閉じます。
     */
    protected void closeChannel() {
        if (stateBuffer[SEND_CHANNEL_INDEX] == 0) {
            return;
        }

        try {
            ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
            ph.initCloseChannel(stateBuffer[SEND_CHANNEL_INDEX]);
            stateBuffer[SEND_CHANNEL_INDEX] = 0;
            ph.send();
        } catch (Exception e) {
            // 既にcloseしていた場合
        }
    }

    /**
     * 指定したチャネルを閉じます。
     *
     * @param channel チャネルID
     */
    private void closeChannel(byte channel) {
        try {
            ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
            ph.initCloseChannel(channel);
            ph.send();
        } catch (Exception e) {
            // 既にcloseしていた場合
        }
    }

    /**
     * すべてのチャネルを閉じます。
     */
    private void closeAllChannels() {
        closeChannel(stateBuffer[PROTOCOL_CHANNEL_INDEX]);
        closeChannel();
    }

    private static Client instance;

    /**
     * HttpClientのインスタンスを取得します。
     *
     * @return HttpClientのインスタンス
     * @throws SystemException システム例外
     */
    public static Client getInstance() throws SystemException {
        if (Client.instance == null) {
            Client.instance = new Client();
        }
        return Client.instance;
    }
}

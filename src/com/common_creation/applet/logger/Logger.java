package com.common_creation.applet.logger;

import javacard.framework.*;
import uicc.access.UICCException;
import uicc.toolkit.*;

/**
 * ログを送信するためのAppletクラス。
 * このクラスは、メニューからログを送信する機能を提供します。
 */
public class Logger extends Applet implements ToolkitInterface, ToolkitConstants {
    private ToolkitRegistry toolkitRegistry;
    private byte menuId;
    private final byte[] menuText = {'S', 'e', 'n', 'd', ' ', 'L', 'o', 'g'};
    private final byte[] readBuffer;
    private final byte[] tmpBuffer;
    private static final byte[] channelStatusText = {'C', 'h', 'a', 'n', 'n', 'e', 'l', 'S', 't', 'a', 't', 'u', 's' };
    private static final byte[] receiveDataErrorText = {'R', 'e', 'c', 'e', 'i', 'v', 'e', 'D', 'a', 't', 'a', 'F', 'a', 'i', 'l', 'e','d' };

    private final byte[] ipAddress;
    private short port = 80;
    private byte[] path;
    private static final byte TRUE = 0x01;
    private static final byte FALSE = 0x00;
    private final byte[] initialized;

    // NOTE: TEST用のPayload,Path,IPアドレス
    private static final byte[] testPayload = {'{', '"', 't', 'e', 'x', 't', '"', ':', '"', 't', 'e', 's', 't', '"', '}'};
    private static final byte[] testPath = {'/', 'v', '1', '/', 'l', 'o', 'g'};
    private static final byte[] testServerAddress = {(byte) 0x00, (byte) 0x71, (byte) 0xAE, (byte) 0x96};

    /**
     * インストール時のパラメータ処理を行います。
     * このアプレットではインストールパラメータを使用しないため未実装
     *
     * @param bArray インストールパラメータが含まれるバイト配列
     * @param bOffset パラメータのオフセット
     * @param bLength パラメータの長さ
     * @throws ISOException ISO例外
     */
    private static void processInstallParameters(byte[] bArray, short bOffset, byte bLength) throws ISOException {

    }

    /**
     * Loggerのコンストラクタ。
     */
    private Logger() {
        readBuffer = JCSystem.makeTransientByteArray((short) 640, JCSystem.CLEAR_ON_RESET);
        tmpBuffer = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_RESET);
        ipAddress = JCSystem.makeTransientByteArray((short) 4, JCSystem.CLEAR_ON_RESET);
        initialized = JCSystem.makeTransientByteArray((short) 1, JCSystem.CLEAR_ON_RESET);
        initialized[0] = FALSE;
    }

    /**
     * アプレットをインストールした際に呼ばれるメソッド。
     *
     * @param bArray インストールパラメータが含まれるバイト配列
     * @param bOffset パラメータのオフセット
     * @param bLength パラメータの長さ
     * @throws ISOException ISO例外
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
        processInstallParameters(bArray, bOffset, bLength);
        Logger logger = new Logger();

        logger.register();

        logger.toolkitRegistry = ToolkitRegistrySystem.getEntry();
        logger.toolkitRegistry.setEvent(ToolkitConstants.EVENT_EVENT_DOWNLOAD_DATA_AVAILABLE);

        logger.menuId = logger.toolkitRegistry.initMenuEntry(logger.menuText, (short) 0, (short) logger.menuText.length, (byte) 0, false, (byte) 0, (short) 0);

    }

    /**
     * シェア可能なインターフェースオブジェクトを取得します。
     * ここでは使用しないため未実装
     *
     * @param clientAID クライアントのAID
     * @param parameter パラメータ
     * @return シェア可能なインターフェースオブジェクト
     */
    public Shareable getShareableInterfaceObject(AID clientAID, byte parameter) {
        return this;
    }

    /**
     * APDUコマンドを処理します。
     * ここでは使用しないため未実装
     *
     * @param apdu APDUオブジェクト
     * @throws ISOException ISO例外
     */
    public void process(APDU apdu) throws ISOException {
    }

    /**
     * Toolkitイベントを処理します。
     *
     * @param event イベントコード
     * @throws ToolkitException Toolkit例外
     */
    public void processToolkit(short event) throws ToolkitException {
        if (event == EVENT_MENU_SELECTION) {
            EnvelopeHandler eh = EnvelopeHandlerSystem.getTheHandler();
            byte selectedId = eh.getItemIdentifier();
            if (selectedId == menuId) {
                this.sendTestLog();
            }
        }

        if (event == EVENT_EVENT_DOWNLOAD_DATA_AVAILABLE) {
            try {
                EnvelopeHandler eh = EnvelopeHandlerSystem.getTheHandler();
                byte channelId = eh.getChannelIdentifier();
                Client http = Client.getInstance();
                if (channelId != http.getBIPChannelID()) {
                    return; // HTTP応答ではない
                }

                short channelStatus = eh.getChannelStatus(channelId);
                short length = (short) (tmpBuffer[0] & 0xff);

                short readOffset = 0;

                if ((channelStatus & 0xff) != 0) {
                    Screen.displayError(channelStatusText, channelStatus);
                } else {
                    byte readLength;
                    final short MAX_READ_SIZE = 0xA0;
                    ProactiveHandler ph = ProactiveHandlerSystem.getTheHandler();
                    ProactiveResponseHandler rh = ProactiveResponseHandlerSystem.getTheHandler();

                    while (length > 0) {
                        if (length > MAX_READ_SIZE) {
                            readLength = (byte) MAX_READ_SIZE;
                        } else {
                            readLength = (byte) (length & 0xff);
                        }
                        ph.init(PRO_CMD_RECEIVE_DATA, (byte) 0x00, (byte) (DEV_ID_CHANNEL_BASE + channelId));
                        ph.appendTLV(ToolkitConstants.TAG_CHANNEL_DATA_LENGTH, readLength);
                        ph.send();
                        byte res = rh.getGeneralResult();
                        if (res != RES_CMD_PERF) {
                            Screen.displayError(receiveDataErrorText, res);
                            break;
                        } else {
                            length = (short) (tmpBuffer[0] & 0xff);
                            readOffset = rh.findAndCopyValue(TAG_CHANNEL_DATA, readBuffer, readOffset);
                        }
                    }
                    if (channelId == http.getBIPChannelID()) {
                        http.closeChannel();
                    }
                }
                short textLength = readOffset;
                if (textLength > (short)20) {
                    textLength = 20;
                }
                Screen.displayText(readBuffer, (short) 0, textLength);

            } catch (ArrayIndexOutOfBoundsException e) {
                Screen.displayText(ExceptionName.ArrayIndexOutOfBoundsException);
            } catch (Exception e) {
                Screen.displayText(ExceptionName.Exception);
            }
        }
    }

    /**
     * Loggerを初期化します。
     *
     * @param ipAddress IPアドレスのバイト配列
     * @param path リクエストパスのバイト配列
     * @param port ポート番号
     */
    public void initLogger(
        byte[] ipAddress,
        byte[] path,
        short port
    ) {
        setIpAddress(ipAddress);
        this.path = path;
        setPort(port);
        if (initialized[0] == FALSE) {
            initialized[0] = TRUE;
        }
    }

    /**
     * Loggerが初期化されているかを確認します。
     *
     * @return 初期化されている場合はtrue、そうでない場合はfalse
     */
    public boolean isInitializedLogger() {
        return initialized[0] == TRUE;
    }
    /**
     * IPアドレスを設定します。
     *
     * @param buffer IPアドレスのバイト配列
     */
    public void setIpAddress(byte[] buffer) {
        if ((short) buffer.length != (short) 4) {
            Screen.displayText(ErrorMessage.invalidIpAddressLength);
            return;
        }
        Util.arrayCopyNonAtomic(buffer, (short) 0, ipAddress, (short) 0, (short) buffer.length);
    }

    /**
     * ポート番号を設定します。
     *
     * @param port ポート番号
     */
    private void setPort(short port) {
        this.port = port;
    }

    /**
     * 送信エラーを表示します。
     *
     * @param message エラーメッセージ
     * @param reason エラーの理由コード
     */
    private void showSendError(byte[] message, short reason) {
        short len = Util.arrayCopy(ErrorMessage.sendError, (short) 0, tmpBuffer, (short) 0, (short) ErrorMessage.sendError.length);
        tmpBuffer[len++] = ':';
        len = Util.arrayCopy(message, (short) 0, tmpBuffer, len, (short) message.length);
        tmpBuffer[len++] = ':';
        len = HexUtil.numToCharArray(reason, tmpBuffer, len);
        Screen.displayText(tmpBuffer, len);
    }

    /**
     * 送信エラーを表示します。
     *
     * @param message エラーメッセージ
     */
    private void showSendError(byte[] message) {
        short len = Util.arrayCopy(ErrorMessage.sendError, (short) 0, tmpBuffer, (short) 0, (short) ErrorMessage.sendError.length);
        tmpBuffer[len++] = ':';
        len = Util.arrayCopy(message, (short) 0, tmpBuffer, len, (short) message.length);
        Screen.displayText(tmpBuffer, len);
    }

    /**
     * 指定されたペイロードを送信します。
     *
     * @param payload 送信するペイロード
     * @param offset オフセット
     * @param length 長さ
     */
    private void send(byte[] payload, short offset, short length) {
        try {
            if (isInitializedLogger()) {
                showSendError(ErrorMessage.initializedError);
                return;
            }
            Client.getInstance().post(ipAddress, null, port, path, (short) path.length, payload, offset, length);
        } catch (APDUException e) {
            showSendError(ExceptionName.APDUException, e.getReason());
        } catch (ToolkitException e) {
            showSendError(ExceptionName.ToolkitException, e.getReason());
        } catch (UICCException e) {
            showSendError(ExceptionName.UICCException, e.getReason());
        } catch (NullPointerException e) {
            showSendError(ExceptionName.NullPointerException);
        } catch (ArrayIndexOutOfBoundsException e) {
            showSendError(ExceptionName.ArrayIndexOutOfBoundsException);
        } catch (SystemException e) {
            showSendError(ExceptionName.SystemException, e.getReason());
        } catch (Exception e) {
            showSendError(ExceptionName.Exception);
        }
    }

    /**
     * ログを送信します。
     *
     * @param payload ログデータ
     */
    public void sendLog(byte[] payload) {
        sendLog(payload, (short) 0, (short) payload.length);
    }

    /**
     * ログを送信します。
     *
     * @param payload ログデータ
     * @param offset オフセット
     * @param length 長さ
     */
    public void sendLog(byte[] payload, short offset, short length) {
        send(payload, offset, length);
    }

    /**
     * テスト用のログを送信します。
     * SIM Tool KitでSend Logを選択した場合に呼ばれます。
     */
    private void sendTestLog() {
        try {
            if (isInitializedLogger()) {
                byte[] initText = {'R', 'u', 'n', ' ', 'I', 'n', 'i', 't'};
                Screen.displayText(initText);
                initLogger(testServerAddress, testPath, port);
            } else {
                byte[] initText = {'I', 'n', 'i', 't', 'i', 'a', 'l', 'i', 'z', 'e', 'd'};
                Screen.displayText(initText);
            }
            sendLog(testPayload);
        } catch (APDUException e) {
            Screen.displayError(ExceptionName.APDUException, e.getReason());
        } catch (ToolkitException e) {
            Screen.displayError(ExceptionName.ToolkitException, e.getReason());
        } catch (UICCException e) {
            Screen.displayError(ExceptionName.UICCException, e.getReason());
        } catch (NullPointerException e) {
            Screen.displayText(ExceptionName.NullPointerException);
        } catch (ArrayIndexOutOfBoundsException e) {
            Screen.displayText(ExceptionName.ArrayIndexOutOfBoundsException);
        } catch (SystemException e) {
            Screen.displayError(ExceptionName.SystemException, e.getReason());
        } catch (Exception e) {
            Screen.displayText(ExceptionName.Exception);
        }
    }
}

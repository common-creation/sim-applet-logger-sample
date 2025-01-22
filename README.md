# sim-applet-logger

## 概要

[NTTコミュニケーションズ株式会社](https://www.ntt.com/)より提供されている、[IoT Connect Mobile Type S](https://sdpf.ntt.com/services/icms/)のSIMカードにインストール可能なLoggerアプレットです。  
Windows環境の[IntelliJ IDEA](https://www.jetbrains.com/ja-jp/idea/) CommunityまたはUltimateに対応しています。

## 機能

指定した文字列をログとして、特定のHTTPサーバに送信するサンプルアプレットです。

- Logger : Loggerアプレット本体のクラス
- Client : HTTP通信を行うクライアントクラス
- Screen : 画面表示用のユーティリティクラス
- HexUtil : HEX操作のためのユーティリティクラス
- ErrorMessage : エラーメッセージを定義するクラス
- ExceptionName : 例外メッセージを定義するクラス

## 開発・検証

> [!WARNING]  
> このリポジトリのサブモジュールに含まれるJCDKとGlobalPlatform APIは、それぞれOracle社、GlobalPlatform社の著作物です。 
> 利用する場合は、 `Java Card Development Kit Tools (06_July_2021)` の利用規約及び `GLOBALPLATFORM LICENSE AGREEMENT` に同意する必要があります。  
> https://github.com/martinpaljak/oracle_javacard_sdks/tree/e305a1a0b9bf6b9a8b0c91a9aad8d73537e7ff1b/jc310r20210706_kit/legal
> https://github.com/OpenJavaCard/globalplatform-exports/blob/master/GP-Specification-License-Agreement.pdf

1. このリポジトリをサブモジュールを含めてcloneする

```powershell
git clone --recursive 'https://github.com/common-creation/sim-applet-logger-sample.git'
```

2. [lib/download_apifiles.bat](./lib/download_apifiles.bat)を実行して、UICC Toolkitをダウンロード・展開する

> [!NOTE]  
> このバッチファイルは[NTTコミュニケーションズ株式会社](https://www.ntt.com/)より提供されている[ドキュメント](https://sdpf.ntt.com/services/docs/icms/service-descriptions/applet/sample_applet/sample_applet.html#/api-usim-apiuicc-api)に含まれています。  

```powershell
.\lib\download_apifiles.bat
```

3. IntelliJ IDEAで読み込む

4. Logger.javaの下記の内容を適切なものに書き換える

- testPayload及びtestPathについては、文字列のbyte列として定義する
- testServerAddressにつついては、ipアドレスを16進数で定義する

```java
// NOTE: TEST用のPayload,Path,IPアドレス
private static final byte[] testPayload = {'{', '"', 't', 'e', 'x', 't', '"', ':', '"', 't', 'e', 's', 't', '"', '}'};
private static final byte[] testPath = {'/', 'v', '1', '/', 'l', 'o', 'g'};

// 192.168.1.1を設定する場合
private static final byte[] testServerAddress = {(byte) 0xc0, (byte) 0xa8, (byte) 0x01, (byte) 0x01};

```

5. IntelliJ IDEAの View → Tool Windows → Ant の順に開き`Run Build`をクリックすると、 `.\suncap\logger.cap` が生成される

6. 生成されたcapファイルをSIMカードにインストールする
- インストール方法の詳細については、弊社が公開している[こちらの記事](https://note.com/common_creation/n/nd4bfcd0c0b34)をご参照ください
- C9については、c9.txtに例を記載しております

```text
C900EF08C7020001C8020001EA148012FF0520060100020003000400050006000500
```

7. スマートフォンやIoT機器等にSIMカードを挿入して、SIM Tool Kitのメニューから「Send Log」を押下してください。
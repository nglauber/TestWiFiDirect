TestWiFiDirect
==============

Exemplo simples do uso da API WiFi-Direct do Android.
Mais detalhes em:
http://developer.android.com/guide/topics/connectivity/wifip2p.html

Pré-Requisitos
==============
Android Studio 0.6.1
Dois dispositivos rodando Android 4.0 (API Level 14) ou superior com suporte ao recurso de WiFi-Direct.

Para checar se o aparelho possui esse recurso, acesse as configurações do aparelho e em seguida selecione Wi-Fi. Na parte inferior (normalmente) haverá uma opção Wi-Fi direct.

Instalação
==========
Faça checkout ou baixe o zip do código e importe no Android Studio.
Execute a aplicação em ambos os aparelhos. Se o aparelho estiver conectado a alguma rede wi-fi, desconecte.

Com a aplicação em execução, acesse o menu e clique na opção 'Discover peers'. Faça o mesmo no outro dispositivo.
Após aparecer o nome do dispositivos, selecione a opção de menu 'Connect'.
Aguarde até aparecer a mensagem 'remote: Connected', então você poderá digitar mensagens na caixa de texto localizada na parte inferior e clicar em 'Send'.
A mesma deverá aparecer no outro dispositivo.

Aparelhos
=========
Eu testei esse exemplo com os seguintes aparelhos: 
Motorola Moto G, 
Samsung Galaxy S 4 e 
LG Nexus 4.

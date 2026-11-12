# Validação ao vivo do motor (fork)

O que validar com celulares de verdade, e como, depois das correções do PR jonales/Cobalt#2.
Não roda na nuvem: o proxy bloqueia o WhatsApp. Resultados voltam para `STATUS.md`.

## Caminho recomendado: pela API (msgforge-api)

A API já usa este fork (PR jonales/msgforge-api#12) e tem o roteiro completo, com comandos:
**`msgforge-api/docs/VALIDACAO-COBALT-FORK.md`**. Ele cobre login, aviso de código de segurança,
sessão Signal (`pkmsg` x `msg`), status, mídia e sessão restaurada, e a imagem Docker já instala
este fork no commit fixado em `msgforge-api/scripts/install-cobalt.sh`.

## Caminho curto: só o motor (spike)

Para isolar o motor, sem API nem Docker (precisa de JDK 25 e Maven):

```bash
git clone https://github.com/jonales/Cobalt.git && cd Cobalt
git checkout claude/elegant-faraday-6i4y7z
mvn -DskipTests -Dmaven.javadoc.skip=true install -pl modules/lib -am

cd ../msgforge-api/docs/spikes/cobalt-0.1
mvn -q compile exec:java            # escaneie o QR; comandos: existe | enviar | sair
```

Use a tabela "O que observar" do `README.md` do spike.

## O que medir e onde cada resultado entra no STATUS.md

| Medida | Como ver | Item do STATUS.md |
|---|---|---|
| Tempo do scan até "logado" | relógio; no log, `post-handshake bootstrap complete` | 1.1, 1.2 |
| Aviso "Não foi possível confirmar seu código de segurança" no destinatário | abrir a conversa no celular B | 2.1, 2.4 |
| Envio depois de B responder sai `enc type=msg` (não `pkmsg`) | log `DEBUG` da API (`...COBALT_WIRE=DEBUG`) | 2.6 |
| Ack do envio sem `phash` depois do primeiro reenvio | mesmo log (`ack{...}` sem `phash`) | 2.1 |
| Status entregue/lida chegam | Manager ou `findMessages` da API | (regressão) |

## Se der errado

Guarde o log do trecho (só nomes e atributos dos nós, nunca conteúdo) e anote o número do teste.
Com isso, a próxima sessão do Claude compara com o caminho de envio do motor
(`MessageSender.encryptForDevices`, `MessageEncryption.encryptForDevice`, `IcdcComputer`) e com o
`LiveDeviceService.getDeviceLists`. Registre o achado em `STATUS.md` e numa memória Graphify.

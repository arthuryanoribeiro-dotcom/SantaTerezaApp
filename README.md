# Santa Tereza Nelore – App Android (Avaliação 3 – Programação Mobile / UFMS)

Aplicativo Android nativo em **Java** para a **Fazenda Santa Tereza** (genética Nelore, São Luís de Montes Belos – GO), baseado no site [fazendastatereza.com.br](https://fazendastatereza.com.br/).

Focos principais: **Acessibilidade** e **Usabilidade**.

## Como abrir e rodar

1. Abra o Android Studio → **Open** → selecione a pasta `SantaTerezaApp`.
2. Aguarde o *Gradle Sync* (o Studio baixa o Gradle 8.11.1 e as dependências).
3. Use um emulador **com Google Play** (necessário para a localização) e API 26 ou superior.
4. Rode o módulo `app`.

> Se o Studio oferecer o *AGP Upgrade Assistant*, você pode aceitar ou manter a versão atual (AGP 8.7.3).

### Dicas para testar no emulador
- **Localização:** em *Extended Controls (⋮) → Location*, defina um ponto, por exemplo Goiânia (-16.6869, -49.2648), e toque em **Minha localização** na aba Mapa.
- **Notificações/Alarme:** agende uma visita para daqui a 2 minutos com lembrete "No horário da visita".
- **Câmera:** o emulador tem câmera virtual (cena 3D).
- **Permissões negadas:** *Configurações → Apps → Santa Tereza Nelore → Permissões*.

## Requisitos atendidos

| Requisito | Onde está |
|---|---|
| Java + Empty Views Activity | todo o projeto |
| ≥ 3 telas | Login, Cadastro, Principal (5 abas: Início, Matrizes, Agenda, Mapa, Perfil), Ficha da Matriz, Editar Perfil, Alterar Senha |
| Splash Screen sem atraso artificial | `ui/SplashActivity` + `Theme.SantaTereza.Starting` (SplashScreen API) |
| Ícone personalizado no Manifest | `res/drawable/ic_launcher_foreground.xml`, `mipmap-anydpi-v26` |
| Login, cadastro, sessão e logout | `ui/auth/*`, `seguranca/SessionManager` (EncryptedSharedPreferences), `BaseAutenticadaActivity` |
| Editar perfil | `ui/perfil/EditarPerfilActivity`, `AlterarSenhaActivity` |
| Senha com hash + salt (BCrypt) | `seguranca/SenhaUtil` (biblioteca `at.favre.lib:bcrypt`) |
| Foto de perfil (câmera/galeria) persistida no Room | `ui/perfil/PerfilFragment`, `util/ImagemUtil`, campo `Usuario.fotoPath` |
| Notificações com tratamento de permissão | `notificacao/NotificationHelper`, `MainActivity.talvezPedirNotificacao()` |
| Recurso obrigatório | **Geolocalização** (aba Mapa), **Alarmes** (lembrete de visita) e **API HTTP** (clima, Open-Meteo) |
| Permissões em tempo de execução | Localização, Câmera e Notificações, com tratamento de concessão e negação |
| Banco Room | `data/AppDatabase` (Usuario, Matriz, Favorito, Visita) |

## Estrutura

```
app/src/main/java/br/ufms/santatereza/
├── SantaTerezaApp.java        # Application: canais de notificação, configuração do mapa
├── api/                       # Consumo da API de clima (HttpURLConnection + JSON)
├── data/                      # Room: entidades, DAOs, banco e dados iniciais
├── notificacao/               # Notificações, AlarmManager, receivers
├── seguranca/                 # BCrypt e sessão criptografada
├── ui/                        # Activities e Fragments
└── util/                      # Validação, formatação, imagens, contatos
```

## Observações
- As **matrizes** do catálogo são **ilustrativas**: o site ainda não publica os dados dos animais. O app mostra esse aviso. Para usar dados reais, edite `data/DadosIniciais.java`.
- As **coordenadas da fazenda** (`util/Contatos.java`) são as da sede: 16°20'40.4"S 50°17'45.6"W (-16.344556, -50.296000).
- O mapa usa **OpenStreetMap (osmdroid)**, então não precisa de chave de API.

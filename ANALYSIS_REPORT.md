# Relatório Técnico Completo — BedWars1058 v25.9

> **Data:** 08/06/2026
> **Versão analisada:** 25.9
> **Objetivo:** Análise de arquitetura, compatibilidade Minecraft 1.21.4 e plano de migração

---

## Índice

1. [Arquitetura do Projeto](#1-arquitetura-do-projeto)
2. [Sistema de Version Support](#2-sistema-de-version-support)
3. [Compatibilidade Minecraft 1.21.4](#3-compatibilidade-minecraft-1214)
4. [Migração de Atributos](#4-migração-de-atributos)
5. [Criação do Módulo versionsupport_v1_21_R3](#5-criação-do-módulo-versionsupport_v1_21_r3)
6. [Problemas Maven](#6-problemas-maven)
7. [Plano de Implementação](#7-plano-de-implementação)

---

## 1. ARQUITETURA DO PROJETO

### 1.1 Estrutura Maven

O projeto é um **multi-módulo Maven** com 18 módulos. O root POM (`pom.xml`) declara `<packaging>pom</packaging>` e lista todos os módulos na seção `<modules>`.

### 1.2 Módulos e Responsabilidades

| Módulo | artifactId | Papel |
|--------|-----------|-------|
| `bedwars-api` | `bedwars-api` | API pública para outros plugins. Contém interfaces (`IArena`, `ITeam`, `VersionSupport`), eventos, enums, classes de configuração. |
| `bedwars-plugin` | `bedwars-plugin` | **Plugin principal**. Contém toda a lógica do jogo: arenas, shop, upgrades, listeners, comandos, suporte a terceiros. Gera o JAR final via shade plugin. |
| `versionsupport_common` | `versionsupport-common` | Código compartilhado entre versões: listeners cross-version, lógica de restauração de shop. |
| `versionsupport_1_8_R3` | `versionsupport_1_8_R3` | Suporte Minecraft 1.8.x. Contém NMS entities customizadas (Silverfish, IronGolem, Villager shop). |
| `versionsupport_1_12_R1` | `versionsupport_1_12_R1` | Suporte Minecraft 1.12.x. |
| `versionsupport_v1_16_R3` | `versionsupport_v1_16_R3` | Suporte Minecraft 1.16.x. |
| `versionsupport_v1_17_R1` | `versionsupport_v1_17_R1` | Suporte Minecraft 1.17.x. Primeira versão com Mojang mappings (`net.minecraft.*`). |
| `versionsupport_v1_18_R2` | `versionsupport_v1_18_R2` | Suporte Minecraft 1.18.x. |
| `versionsupport_v1_19_R2` | `versionsupport_v1_19_R2` | Suporte Minecraft 1.19.2. Introduz padrão `despawnable` com factory/provider. |
| `versionsupport_v1_19_R3` | `versionsupport_v1_19_R3` | Suporte Minecraft 1.19.3/4. |
| `versionsupport_v1_20_R1` | `versionsupport_v1_20_R1` | Suporte Minecraft 1.20/1.20.1. |
| `versionsupport_v1_20_R2` | `versionsupport_v1_20_R2` | Suporte Minecraft 1.20.2. |
| `versionsupport_v1_20_R3` | `versionsupport_v1_20_R3` | Suporte Minecraft 1.20.3/1.20.4. Módulo base para v1_20_R4 e v1_21_R3. |
| `versionsupport_v1_20_R4` | `versionsupport_v1_20_R4` | **Stub vazio** — estende v1_20_R3 sem sobrescrever nada. Package diz v1_21_R3 mas está no diretório v1_20_R4. |
| `versionsupport_v1_21_R3` | `versionsupport_v1_21_R3` | **Stub vazio** — estende v1_20_R4 → v1_20_R3. NÃO contém implementação real para 1.21.4. |
| `resetadapter_slime` | `resetadapter-slime` | Reset de mapa usando SlimeWorldManager. |
| `resetadapter_slimepaper` | `resetadapter-slimepaper` | Reset de mapa usando InfernalSuite/ASWM. |
| `resetadapter_aswm` | `resetadapter-aswm` | Reset de mapa usando AdvancedSlimeWorldManager. |

### 1.3 Fluxo de Build

1. `mvn compile` compila todos os módulos na ordem do reactor
2. `bedwars-plugin` usa `maven-shade-plugin` para empacotar dependências (bstats, HikariCP, SLF4J, sidebar)
3. Os módulos `versionsupport_*` e `resetadapter_*` são compilados mas **não** incluídos no shade — são dependências separadas que devem estar no classpath
4. O JAR final do plugin contém apenas o `bedwars-plugin` + `bedwars-api` + libs shaded

### 1.4 Relações entre Módulos

```
bedwars-api (API pública)
    ^
    | depende
    |
bedwars-plugin (Plugin principal)
    |
    |--- depende de todos os versionsupport_*
    |--- depende de todos os resetadapter_*
    |--- depende de bibliotecas externas
    |
versionsupport_common
    ^
    |--- depende de bedwars-api
    |
versionsupport_v1_XX_RX (cada versão)
    |
    |--- depende de bedwars-api (provided)
    |--- depende de versionsupport-common (provided)
    |--- depende de spigot versão específica (provided)
    |
versionsupport_v1_20_R3
    ^
    |--- estendido por
    |
versionsupport_v1_20_R4 (stub vazio)
    ^
    |--- estendido por
    |
versionsupport_v1_21_R3 (stub vazio)
```

---

## 2. SISTEMA DE VERSION SUPPORT

### 2.1 Detecção de Versão

**Arquivo:** `bedwars-plugin/src/main/java/com/andrei1058/bedwars/BedWars.java:125`

```java
private static final String version = Bukkit.getServer().getClass().getName().split("\\.")[3];
```

Isso extrai de `org.bukkit.craftbukkit.v1_20_R3.CraftServer` → `"v1_20_R3"`.

### 2.2 Carregamento Dinâmico

**Arquivo:** `BedWars.java:160-184` (onLoad)

```java
Class supp = Class.forName("com.andrei1058.bedwars.support.version." + version + "." + version);
nms = (VersionSupport) supp.getConstructor(Plugin.class, String.class).newInstance(this, version);
```

**Mecanismo:** Classpath-based discovery. O classname é construído como `{package}.{version}.{version}`:
- Package: `com.andrei1058.bedwars.support.version`
- Ex: para v1_20_R3 → `com.andrei1058.bedwars.support.version.v1_20_R3.v1_20_R3`

### 2.3 Cadeia de Herança

```
VersionSupport (abstract, bedwars-api)
  +-- v1_8_R3
  +-- v1_12_R1
  +-- v1_16_R3
  +-- v1_17_R1
  +-- v1_18_R2
  +-- v1_19_R2
  +-- v1_19_R3
  +-- v1_20_R1
  +-- v1_20_R2
  +-- v1_20_R3
       +-- v1_20_R4 (extends v1_20_R3, 0 overrides)
            +-- v1_21_R3 (extends v1_20_R4, 0 overrides)
```

### 2.4 Version Integer Mapping (`getVersion()`)

| Módulo | getVersion() | Impacto no VersionCommon |
|--------|-------------|--------------------------|
| v1_8_R3 | **0** | < 1: sem listeners registrados |
| v1_12_R1 | **5** | >= 5: listeners 1.13+; = 5: drop behavior 1.12 |
| v1_16_R3 | **8** | >= 5: listeners 1.13+ |
| v1_17_R1 | **8** | Igual v1_16_R3 |
| v1_18_R2 | **8** | Igual v1_16_R3 |
| v1_19_R2 | **9** | >= 9: comportamento moderno |
| v1_19_R3 | **9** | Igual |
| v1_20_R1 | **9** | Igual |
| v1_20_R2 | **9** | Igual |
| v1_20_R3 | **10** | Experimental sidebar score placeholders |
| v1_20_R4 | **10** | Herdado de v1_20_R3 |
| v1_21_R3 | **10** | Herdado de v1_20_R3 (via v1_20_R4) |

### 2.5 Registro de Listeners

Cada módulo chama `new VersionCommon(this)` em `registerVersionListeners()`. O `VersionCommon` usa o inteiro `getVersion()` para decidir quais listeners registrar:

```java
// VersionCommon.java
if (versionSupport.getVersion() > 1) {
    // registra SwapItem, ArrowCollect
}
if (versionSupport.getVersion() < 5) {
    // registra PlayerPickup, PlayerDropPick_1_11Minus
}
if (versionSupport.getVersion() > 5) {
    // registra Interact_1_13Plus, ItemDropPickListener
}
```

### 2.6 Evolução do Registro de Entidades

| Período | Método | Detalhes |
|---------|--------|----------|
| **1.8 - 1.12** | `registerEntity()` via reflection | Usa `EntityTypes` fields/methods diretamente. Registra `Silverfish2` (ID 60), `IGolem` (ID 99) |
| **1.16 - 1.17** | `DataConverterRegistry` + `EntityTypes.Builder` | Usa `DataFixUtils`, `DataConverterTypes.ENTITY` |
| **1.18 - 1.21** | **No-op** `registerEntities()` | Entities são criadas via Bukkit `spawnEntity()` com `EntityType.SILVERFISH` / `EntityType.IRON_GOLEM`, depois customizadas via NMS pathfinding goals |

### 2.7 Padrão Despawnable (v1_19_R2+)

```
DespawnableType (enum)              → IRON_GOLEM, SILVERFISH
DespawnableAttributes (record)      → type, speed, health, damage, despawnSeconds
DespawnableProvider<T> (abstract)   → getType(), spawn(), applyDefaultSettings(), etc.
  +-- TeamIronGolem
  +-- TeamSilverfish
DespawnableFactory                  → List<DespawnableProvider<?>>, spawn(attr, loc, team)
```

---

## 3. COMPATIBILIDADE MINECRAFT 1.21.4

### 3.1 Problema Central

**O módulo `versionsupport_v1_21_R3` é um stub VAZIO.** Ele estende `v1_20_R4` → `v1_20_R3`, mas:

1. O servidor 1.21.4 tem **CraftBukkit** em `org.bukkit.craftbukkit.v1_21_R3.*`
2. O `v1_20_R3` importa `org.bukkit.craftbukkit.v1_20_R3.*` — **não existe no classpath 1.21.4**
3. As NMS obfuscated method/field names (ex: `tag.k(...)`, `GenericAttributes.a`, `entityLiving.bP`) mudam a cada versão

### 3.2 Tabela de Incompatibilidades

| Arquivo | Classe | Linha | Problema | Impacto | Solução Recomendada |
|---------|--------|-------|----------|---------|-------------------|
| `v1_20_R3.java` | `v1_20_R3` | 44 | `import org.bukkit.craftbukkit.v1_20_R3.*` | ClassNotFoundException no 1.21.4 | Trocar para `org.bukkit.craftbukkit.v1_21_R3.*` |
| `v1_20_R3.java` | `v1_20_R3` | 266 | `tag.k("generic.attackDamage")` | Método `k` pode não existir | `tag.getDouble("generic.attackDamage")` |
| `v1_20_R3.java` | `v1_20_R3` | 166 | `EntityTNTPrimed.class.getDeclaredField("g")` | Field name `g` muda por versão | Verificar field name no descompilador 1.21.4 |
| `v1_20_R3.java` | `v1_20_R3` | 283 | `player.a(player.dN().m(), 1000)` | Métodos obsoletos | `player.hurt(player.damageSources().voidDamage(), 1000)` |
| `v1_20_R3.java` | `v1_20_R3` | 335 | `BlockBase.class.getDeclaredField("aH")` | Field name `aH` muda | Atualizar para field name de 1.21.4 |
| `v1_20_R3.java` | `v1_20_R3` | 338-362 | `Blocks.fz`, `Blocks.aQ`, `Blocks.ej` etc | Block constants mudam | Mapear para constantes de 1.21.4 |
| `v1_20_R3.java` | `v1_20_R3` | 655 | `fb.b`, `fb.c`, `fb.d` (Fireball direction) | Field names mudam | `fb.xPower`, `fb.yPower`, `fb.zPower` |
| `v1_20_R3.java` | `v1_20_R3` | 665 | `ParticleParamRedstone(Vector3f, float)` | Construtor pode mudar | Verificar API 1.21.4 |
| `v1_20_R3.java` | `v1_20_R3` | 289-293 | `EnumItemSlot.f/e/d/c` | Slots podem mudar | Verificar `EnumItemSlot` em 1.21.4 |
| `v1_20_R3.java` | `v1_20_R3` | 829 | `connection.b(packet)` | Método `b` obsoleto | `connection.send(packet)` |
| `DespawnableProvider.java` | `DespawnableProvider` | 34 | `entityLiving.bP` (target selector) | Field name muda | Usar método getter ou verificar field |
| `DespawnableProvider.java` | `DespawnableProvider` | 38 | `entityLiving.bO` (goal selector) | Field name muda | Usar método getter ou verificar field |
| `DespawnableProvider.java` | `DespawnableProvider` | 42-43 | `bO.b().clear()` / `bP.b().clear()` | Método `b()` muda | Atualizar para API 1.21.4 |
| `TeamSilverfish.java` | `TeamSilverfish` | 15 | `import org.bukkit.craftbukkit.v1_20_R3.*` | ClassNotFoundException | Trocar para `v1_21_R3` |
| `TeamIronGolem.java` | `TeamIronGolem` | 15 | `import org.bukkit.craftbukkit.v1_20_R3.*` | ClassNotFoundException | Trocar para `v1_21_R3` |
| `v1_20_R3.java` | `v1_20_R3` | 715 | `itemStack.v()` (getTag) | Method name `v` muda | `itemStack.getTag()` |
| `v1_20_R3.java` | `v1_20_R3` | 736 | `itemStack.c(tag)` (setTag) | Method name `c` muda | `itemStack.setTag(tag)` |
| `v1_20_R3.java` | `v1_20_R3` | 695 | `i.d()` (getItem) | Method name `d` muda | `i.getItem()` |
| `v1_20_R3.java` | `v1_20_R3` | 706 | `i.H()` (getEntity) | Method name `H` muda | `i.getEntity()` |
| `v1_20_R3.java` | `v1_20_R3` | 625 | `a().m` (level name) | Field `m` muda | Verificar API `DedicatedServer` |

### 3.3 Bukkit API — Itens a Verificar

| API | Status | Arquivos Afetados |
|-----|--------|-------------------|
| `ItemFlag.HIDE_ATTRIBUTES` | Disponível, mas pode precisar de `HIDE_ADDITIONAL_TOOLTIP` | ArenaGUI.java:172, Misc.java:307, ShopManager.java:420 |
| `PotionEffectType.INVISIBILITY` | Disponível | v1_20_R3.java:223 |
| `SkullMeta.setOwnerProfile()` | Disponível | v1_20_R3.java:534 |
| `Particle.VILLAGER_HAPPY` | Disponível | v1_20_R3.java:825 |
| `Material.POTION` | Disponível | v1_20_R3.java:219 |
| `Villager` entity | Disponível, AI pode ter mudado | v1_20_R3.java:235 |
| `EnderDragon.Phase.CIRCLING` | Disponível | v1_20_R3.java:315 |
| `ArmorStand` | Disponível | v1_20_R3.java:272 |
| `Fireball` class | Disponível | v1_20_R3.java:653-658 |
| `BlockData` (Bed, Ladder, WallSign) | Disponível | v1_20_R3.java:322-328, 637-640, 800-820 |

---

## 4. MIGRAÇÃO DE ATRIBUTOS

### 4.1 Uso de `GenericAttributes` (NMS)

Em `DespawnableProvider.java` (presente em v1_19_R2, v1_19_R3, v1_20_R1, v1_20_R2, v1_20_R3):

```java
// Linhas 67-69
Objects.requireNonNull(entity.a(GenericAttributes.a)).a(attr.health());
Objects.requireNonNull(entity.a(GenericAttributes.d)).a(attr.speed());
Objects.requireNonNull(entity.a(GenericAttributes.c)).a(attr.damage());
```

#### Tabela de Mapeamento

| Código Atual (obfuscado) | Mojang Name (1.17-1.20.4) | 1.21.4 (se Mojang) | 1.21.4 (se obfuscado) |
|---|---|---|---|
| `GenericAttributes.a` | `GenericAttributes.MAX_HEALTH` | `GenericAttributes.MAX_HEALTH` | Pode ser `GenericAttributes.b` |
| `GenericAttributes.d` | `GenericAttributes.MOVEMENT_SPEED` | `GenericAttributes.MOVEMENT_SPEED` | Pode ser `GenericAttributes.e` |
| `GenericAttributes.c` | `GenericAttributes.ATTACK_DAMAGE` | `GenericAttributes.ATTACK_DAMAGE` | Pode ser `GenericAttributes.d` |
| `entity.a(...)` | `entity.getAttribute(...)` | `entity.getAttribute(...)` | Pode ser `entity.b(...)` |
| `.a(value)` | `.setBaseValue(value)` | `.setBaseValue(value)` | Pode ser `.b(value)` |

> **OBS:** Se o Spigot 1.21.4 do CodeMC usa Mojang mappings (reobfuscated), os nomes são `MAX_HEALTH`, `MOVEMENT_SPEED`, `ATTACK_DAMAGE` (estáveis desde 1.17). Se usa nomes obfuscados crus, as letras mudam a cada versão.

### 4.2 Uso de `"generic.attackDamage"` (NBT string)

Em todos os `vX_RX.java` via `getDamage()`:

```java
return tag.k("generic.attackDamage");
```

O método `k()` é a versão obfuscada de `getDouble()`. A string `"generic.attackDamage"` **permanece a mesma** em NBT — é a chave do atributo no compound.

#### Tabela de Correções

| Arquivo | Linha | Código Atual | Código Correto (se Mojang mapped) |
|---------|-------|-------------|-----------------------------------|
| `versionsupport_v1_20_R3/v1_20_R3.java` | 266 | `tag.k("generic.attackDamage")` | `tag.getDouble("generic.attackDamage")` |
| `versionsupport_v1_20_R2/v1_20_R2.java` | 266 | `tag.k("generic.attackDamage")` | `tag.getDouble("generic.attackDamage")` |
| `versionsupport_v1_20_R1/v1_20_R1.java` | 268 | `tag.k("generic.attackDamage")` | `tag.getDouble("generic.attackDamage")` |
| `versionsupport_v1_19_R3/v1_19_R3.java` | 264 | `tag.k("generic.attackDamage")` | `tag.getDouble("generic.attackDamage")` |
| `versionsupport_v1_19_R2/v1_19_R2.java` | 271 | `tag.k("generic.attackDamage")` | `tag.getDouble("generic.attackDamage")` |
| `versionsupport_v1_18_R2/v1_18_R2.java` | 272 | `compound.k("generic.attackDamage")` | `compound.getDouble("generic.attackDamage")` |
| `versionsupport_v1_17_R1/v1_17_R1.java` | 293 | `compound.getDouble("generic.attackDamage")` | ✅ Já correto |
| `versionsupport_v1_16_R3/v1_16_R3.java` | 265 | `compound.getDouble("generic.attackDamage")` | ✅ Já correto |
| `versionsupport_1_12_R1/v1_12_R1.java` | 267 | `compound.getDouble("generic.attackDamage")` | ✅ Já correto |
| `versionsupport_1_8_R3/v1_8_R3.java` | 305 | `compound.getDouble("generic.attackDamage")` | ✅ Já correto |

### 4.3 Bukkit API `Attribute` / `AttributeModifier`

**NENHUM uso encontrado** de `org.bukkit.attribute.Attribute` ou `org.bukkit.attribute.AttributeModifier` em todo o projeto. O BedWars1058 **não usa** a Bukkit Attribute API. Toda manipulação de atributos é via NMS (`GenericAttributes`).

### 4.4 Resumo da Migração de Atributos

| Atributo | Chave NBT (antiga) | Chave NBT (nova) | Status |
|----------|-------------------|------------------|--------|
| attackDamage | `generic.attackDamage` | `generic.attackDamage` | Mesma — não mudou |
| maxHealth | Não usado | — | Sem impacto |
| followRange | Não usado | — | Sem impacto |
| knockbackResistance | Não usado | — | Sem impacto |
| movementSpeed | Não usado | — | Sem impacto |
| armor | Não usado | — | Sem impacto |
| armorToughness | Não usado | — | Sem impacto |
| attackSpeed | Não usado | — | Sem impacto |

---

## 5. CRIAÇÃO DO MÓDULO VERSIONSUPPORT_V1_21_R3

### 5.1 Análise do Módulo Atual

O `versionsupport_v1_21_R3` atual é um **stub quebrado**:
- `v1_21_R3.java` estende `v1_20_R4` que estende `v1_20_R3` — sem sobrescrever **nenhum** método
- Package do source: `com.andrei1058.bedwars.support.version.v1_21_R3` (correto)
- POM referencia `versionsupport_v1_20_R3` versão `26.6` (inexistente)
- Dependências usam `org.bukkit.craftbukkit.v1_20_R3.*` (inexistente em 1.21.4)

### 5.2 Arquivos a Criar (Copiar de v1_20_R3 e Adaptar)

| # | Arquivo | Origem | Ação |
|---|---------|--------|------|
| 1 | `versionsupport_v1_21_R3/src/.../v1_21_R3/v1_21_R3.java` | `v1_20_R3/v1_20_R3.java` | **REESCREVER** — mudar imports, method names, field names |
| 2 | `versionsupport_v1_21_R3/src/.../v1_21_R3/despawnable/DespawnableType.java` | Copiar de v1_20_R3 | COPIAR (sem mudanças) |
| 3 | `versionsupport_v1_21_R3/src/.../v1_21_R3/despawnable/DespawnableAttributes.java` | Copiar de v1_20_R3 | COPIAR (sem mudanças) |
| 4 | `versionsupport_v1_21_R3/src/.../v1_21_R3/despawnable/DespawnableFactory.java` | Copiar de v1_20_R3 | COPIAR + mudar imports |
| 5 | `versionsupport_v1_21_R3/src/.../v1_21_R3/despawnable/DespawnableProvider.java` | Copiar de v1_20_R3 | COPIAR + mudar imports + GenericAttributes |
| 6 | `versionsupport_v1_21_R3/src/.../v1_21_R3/despawnable/TeamSilverfish.java` | Copiar de v1_20_R3 | COPIAR + mudar imports |
| 7 | `versionsupport_v1_21_R3/src/.../v1_21_R3/despawnable/TeamIronGolem.java` | Copiar de v1_20_R3 | COPIAR + mudar imports |

### 5.3 Mudanças de Import (v1_20_R3 → v1_21_R3)

| Import Antigo | Import Novo |
|--------------|-------------|
| `org.bukkit.craftbukkit.v1_20_R3.CraftServer` | `org.bukkit.craftbukkit.v1_21_R3.CraftServer` |
| `org.bukkit.craftbukkit.v1_20_R3.entity.*` | `org.bukkit.craftbukkit.v1_21_R3.entity.*` |
| `org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack` | `org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack` |
| `org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity` | `org.bukkit.craftbukkit.v1_21_R3.entity.CraftEntity` |

### 5.4 NMS Method/Field Names Ofuscados — Mapeamento Suspeito

> **Nota:** Estes mapeamentos são baseados em padrões históricos. A verificação definitiva requer descompilar o `spigot-1.21.4-R0.1-SNAPSHOT.jar`.

| v1_20_R3 (obfuscado) | Provável Mojang 1.21.4 | Provável obfuscado 1.21.4 | Localização |
|-----------------------|----------------------|--------------------------|-------------|
| `NBTTagCompound.k(String)` | `getDouble(String)` | `l(String)` | `getDamage()` |
| `NBTTagCompound.e(String)` | `hasKey(String)` | `f(String)` | `isCustomBedWarsItem()` |
| `NBTTagCompound.l(String)` | `getString(String)` | `m(String)` | `getCustomData()` |
| `NBTTagCompound.a(String, String)` | `putString(String, String)` | `b(String, String)` | `setTag()` |
| `ItemStack.v()` | `getTag()` | `w()` | `getTag()` |
| `ItemStack.c(NBTTagCompound)` | `setTag(NBTTagCompound)` | `d(NBTTagCompound)` | `applyTag()` |
| `Item.d()` | `getItem()` | `e()` | `getItem()` |
| `ItemStack.H()` | `getEntity()` | `I()` | `getEntity()` |
| `EntityTNTPrimed.g` | `owner` field | `h` | `setSource()` |
| `BlockBase.aH` | `explosionResistance` | `aI` | `registerTntWhitelist()` |
| `GenericAttributes.a` | `MAX_HEALTH` | `b` | `applyDefaultSettings()` |
| `GenericAttributes.d` | `MOVEMENT_SPEED` | `e` | `applyDefaultSettings()` |
| `GenericAttributes.c` | `ATTACK_DAMAGE` | `d` | `applyDefaultSettings()` |
| `entityLiving.bP` | `targetSelector` | `bQ` | `getTargetSelector()` |
| `entityLiving.bO` | `goalSelector` | `bP` | `getGoalSelector()` |
| `EntityPlayer.dN()` | `damageSources()` | `dO()` | `voidKill()` |
| `PlayerConnection.c` | `connection` | `d` | `sendPacket()` |
| `PlayerConnection.b(Packet)` | `send(Packet)` | `c(Packet)` | `sendPacket()` |
| `DedicatedServer.a()` | `getProperties()` | `b()` | `getMainLevel()` |
| `.m` (level name) | `levelName` | `n` | `getMainLevel()` |

### 5.5 Métodos a Reescrever

| Método | Descrição da Mudança |
|--------|---------------------|
| `getDamage()` | `tag.k(...)` → metodo atualizado |
| `setSource()` | Reflection field `"g"` → field de 1.21.4 |
| `voidKill()` | `player.a(player.dN().m(), 1000)` → nova API DamageSource |
| `hideArmor()` / `showArmor()` | Verificar `PacketPlayOutEntityEquipment` |
| `registerTntWhitelist()` | Reflection em `BlockBase.aH` e constantes `Blocks.*` |
| `sendPacket()` | `connection.b(packet)` → `connection.send(packet)` |
| `getTag()` | `itemStack.v()` → metodo atualizado |
| `getItem()` | `i.d()` → metodo atualizado |
| `getEntity()` | `i.H()` → metodo atualizado |
| `playRedStoneDot()` | Verificar constructor `ParticleParamRedstone` |
| `setFireballDirection()` | `fb.b/c/d` → field names atualizados |
| `getMainLevel()` | `a().m` → field names atualizados |
| `applyDefaultSettings()` | `GenericAttributes.a/d/c` → field names atualizados |
| `clearSelectors()` | `bO.b()`, `bP.b()` → field/method names atualizados |

---

## 6. PROBLEMAS MAVEN

### 6.1 Críticos

| Módulo | Problema | Linha | Código Atual | Correção |
|--------|----------|-------|-------------|----------|
| `versionsupport_v1_21_R3/pom.xml` | Versão inexistente de dependência | 54-55 | `<version>26.6</version>` | `<version>${project.version}</version>` |
| `versionsupport_v1_21_R3/pom.xml` | Versão hardcoded | 48-49 | `<version>25.9</version>` | `<version>${project.version}</version>` |
| `versionsupport_v1_21_R3/` | Módulo inteiro é stub vazio | N/A | `v1_21_R3 extends v1_20_R4` (sem overrides) | Reescrever com implementação real para 1.21.4 |
| `versionsupport_v1_20_R4/` | Package inconsistente | 1 | `package ... v1_21_R3` mas diretório é `v1_20_R4` | Renomear package para `v1_20_R4` ou mover diretório |

### 6.2 Médios

| Módulo | Problema | Correção |
|--------|----------|----------|
| `bedwars-api/pom.xml` | Dependência `spigot:1.16.5-R0.1-SNAPSHOT` com `scope=compile` | Mudar para `scope=provided` |
| `bedwars-api/pom.xml` | Dependência `google-collect:1.0` (obsoleto desde 2010) | Substituir por `com.google.guava:guava` |
| `versionsupport_v1_20_R3/pom.xml` | `sidebar-v1_20_R4` no módulo R3 (inconsistente de versão) | Verificar se é intencional |
| `bedwars-plugin/pom.xml` | `bungeecord-chat` versão `1.18-R0.1-SNAPSHOT` (root POM tem `1.8-SNAPSHOT`) | Unificar versões |
| `versionsupport_v1_19_R2/pom.xml` | `maven.compiler.source=1.17` (formato antigo) | Trocar para `17` |
| `versionsupport_v1_19_R3/pom.xml` | `maven.compiler.source=1.17` (formato antigo) | Trocar para `17` |
| `versionsupport_v1_20_R1/pom.xml` | `maven.compiler.source=19` (Java 19? Deveria ser 17) | Trocar para `17` |
| `versionsupport_v1_20_R2/pom.xml` | `maven.compiler.source=19` (Java 19? Deveria ser 17) | Trocar para `17` |

### 6.3 Dependências Circulares

**Potencial circular**: `versionsupport_v1_21_R3` → depende de `versionsupport_v1_20_R4` → depende de `versionsupport_v1_20_R3`.

Atualmente não há loop porque v1_20_R4 não depende de v1_21_R3. Mas a cadeia de herança cria um acoplamento frágil: qualquer mudança em v1_20_R3 impacta v1_20_R4 e v1_21_R3.

### 6.4 Artefatos Inexistentes

| Dependência | Módulo | Problema |
|------------|--------|----------|
| `versionsupport_v1_20_R3:26.6` | `versionsupport_v1_21_R3` | Versão 26.6 não existe no repositório. Parent é 25.9. |

---

## 7. PLANO DE IMPLEMENTAÇÃO

### 7.1 Ordem de Execução

```
[1] CORRIGIR POM.XML DO VERSIONSUPPORT_V1_21_R3
    ├── Trocar versão de dependência 26.6 → ${project.version}
    └── Trocar versão hardcoded 25.9 → ${project.version}

[2] CRIAR DIRETÓRIO DO PACOTE DESPAWNABLE
    └── mkdir -p versionsupport_v1_21_R3/src/main/java/com/andrei1058/bedwars/support/version/v1_21_R3/despawnable

[3] CRIAR ARQUIVOS DESPAWNABLE
    ├── DespawnableType.java (cópia de v1_20_R3, sem mudanças)
    ├── DespawnableAttributes.java (cópia de v1_20_R3, sem mudanças)
    ├── DespawnableFactory.java (cópia, atualizar imports v1_20_R3 → v1_21_R3)
    ├── DespawnableProvider.java (cópia, atualizar imports + GenericAttributes)
    ├── TeamSilverfish.java (cópia, atualizar imports)
    └── TeamIronGolem.java (cópia, atualizar imports)

[4] CRIAR V1_21_R3.JAVA
    ├── Copiar base de v1_20_R3.java
    ├── Atualizar TODOS os imports: v1_20_R3 → v1_21_R3
    ├── Atualizar NBT method names ofuscados
    ├── Atualizar GenericAttributes field names
    ├── Atualizar voidKill() para API 1.21.4
    ├── Atualizar setSource() reflection field
    ├── Atualizar registerTntWhitelist() reflection fields + block constants
    ├── Atualizar sendPacket() / sendPackets()
    ├── Atualizar setFireballDirection()
    ├── Atualizar getMainLevel()
    └── Atualizar PathfinderGoal selector fields

[5] VERIFICAR DEPENDÊNCIAS NO BEDWARS-PLUGIN
    └── Garantir que versionsupport_v1_21_R3 está listado

[6] COMPILAR E CORRIGIR
    ├── Executar mvn clean compile
    ├── Corrigir erros de compilação
    └── Executar mvn clean package

[7] TESTAR EM PAPER 1.21.4
    └── Validar funcionamento em servidor real
```

### 7.2 Checklist Detalhado

- [ ] **POM.XML**: Corrigir `versionsupport_v1_21_R3/pom.xml` — versão `26.6` → `${project.version}` (linha 55)
- [ ] **POM.XML**: Corrigir `versionsupport_v1_21_R3/pom.xml` — versão hardcoded `25.9` → `${project.version}` (linha 49)
- [ ] **CLASSE**: Criar `versionsupport_v1_21_R3/.../despawnable/DespawnableType.java`
- [ ] **CLASSE**: Criar `versionsupport_v1_21_R3/.../despawnable/DespawnableAttributes.java`
- [ ] **CLASSE**: Criar `versionsupport_v1_21_R3/.../despawnable/DespawnableFactory.java`
- [ ] **CLASSE**: Criar `versionsupport_v1_21_R3/.../despawnable/DespawnableProvider.java`
- [ ] **CLASSE**: Criar `versionsupport_v1_21_R3/.../despawnable/TeamSilverfish.java`
- [ ] **CLASSE**: Criar `versionsupport_v1_21_R3/.../despawnable/TeamIronGolem.java`
- [ ] **CLASSE**: Criar `versionsupport_v1_21_R3/.../v1_21_R3.java` com implementação completa
- [ ] **IMPORTS**: Atualizar `org.bukkit.craftbukkit.v1_20_R3.*` → `v1_21_R3.*` em todos os arquivos
- [ ] **NBT**: `tag.k("generic.attackDamage")` → `tag.getDouble("generic.attackDamage")`
- [ ] **NBT**: `tag.e(String)` → `tag.hasKey(String)` ou obfuscado equivalente
- [ ] **NBT**: `tag.l(String)` → `tag.getString(String)` ou obfuscado equivalente
- [ ] **NBT**: `tag.a(String, String)` → `tag.putString(String, String)` ou obfuscado equivalente
- [ ] **NBT**: `itemStack.v()` → `itemStack.getTag()` ou obfuscado equivalente
- [ ] **NBT**: `itemStack.c(tag)` → `itemStack.setTag(tag)` ou obfuscado equivalente
- [ ] **ITEM**: `i.d()` → `i.getItem()` ou obfuscado equivalente
- [ ] **ITEM**: `i.H()` → `i.getEntity()` ou obfuscado equivalente
- [ ] **ATTRIBUTES**: `GenericAttributes.a` → `GenericAttributes.MAX_HEALTH` ou obfuscado equivalente
- [ ] **ATTRIBUTES**: `GenericAttributes.d` → `GenericAttributes.MOVEMENT_SPEED` ou obfuscado equivalente
- [ ] **ATTRIBUTES**: `GenericAttributes.c` → `GenericAttributes.ATTACK_DAMAGE` ou obfuscado equivalente
- [ ] **VOID_KILL**: `player.a(player.dN().m(), 1000)` → API 1.21.4
- [ ] **TNT_SOURCE**: Reflection field `"g"` → field name de 1.21.4
- [ ] **TNT_BLAST**: `BlockBase.aH` → field name de 1.21.4
- [ ] **BLOCKS**: `Blocks.fz`, `Blocks.aQ`, `Blocks.ej` etc → constantes de 1.21.4
- [ ] **PACKET**: `connection.b(packet)` → `connection.send(packet)`
- [ ] **FIREBALL**: `fb.b`, `fb.c`, `fb.d` → field names de 1.21.4
- [ ] **LEVEL**: `a().m` (level name) → field de 1.21.4
- [ ] **GOAL_SELECTOR**: `entityLiving.bO` → field de 1.21.4
- [ ] **TARGET_SELECTOR**: `entityLiving.bP` → field de 1.21.4
- [ ] **GOAL_CLEAR**: `bO.b().clear()` → metodo de 1.21.4
- [ ] **TARGET_CLEAR**: `bP.b().clear()` → metodo de 1.21.4
- [ ] **PARTICLE**: Verificar `ParticleParamRedstone` constructor
- [ ] **EQUIPMENT**: Verificar `PacketPlayOutEntityEquipment` API
- [ ] **ENUM_SLOT**: Verificar `EnumItemSlot.f/e/d/c`
- [ ] **BUILD**: `mvn clean compile` — sem erros
- [ ] **PACKAGE**: `mvn clean package` — JAR gerado
- [ ] **TEST**: Servidor Paper 1.21.4 funcional

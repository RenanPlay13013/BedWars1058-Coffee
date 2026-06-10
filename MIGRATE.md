# Migração para sidebar-dist

## O que mudou

Antes o projeto consumidor precisava de múltiplas dependências:

```xml
<!-- ANTIGO (várias deps) -->
<dependency>
    <groupId>com.andrei1058.spigot.sidebar</groupId>
    <artifactId>sidebar-base</artifactId>
    <version>25.2.2-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>com.andrei1058.spigot.sidebar</groupId>
    <artifactId>sidebar-v1_8_R3</artifactId>
    <version>25.2.2-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
<dependency>
    <groupId>com.andrei1058.spigot.sidebar</groupId>
    <artifactId>sidebar-v1_12_R1</artifactId>
    <version>25.2.2-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
<!-- ... e assim por diante para cada versão suportada -->
```

Agora uma única dependência substitui todas:

```xml
<!-- NOVO (dep única) -->
<dependency>
    <groupId>com.andrei1058.spigot.sidebar</groupId>
    <artifactId>sidebar-dist</artifactId>
    <version>25.2.2-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

O `sidebar-dist` já contém todas as implementações NMS (1.8 a 1.21.4) + API empacotadas em um único JAR via `maven-shade-plugin`.

## Passos da migração

### 1. Substituir as dependências

No `pom.xml` do projeto consumidor:

- **Remover** todas as entradas `sidebar-base`, `sidebar-v1_*`, `sidebar-cmn1`
- **Adicionar** a entrada `sidebar-dist` acima

### 2. Verificar shade no consumidor

Se o projeto consumidor também usa `maven-shade-plugin`, **não é mais necessário** incluir os módulos de versão individualmente. Apenas shader o `sidebar-dist`:

```xml
<configuration>
    <artifactSet>
        <includes>
            <include>com.andrei1058.spigot.sidebar:sidebar-dist</include>
        </includes>
    </artifactSet>
</configuration>
```

O `sidebar-dist` já vem pré-shaded com tudo dentro.

### 3. Repositório

O repositório Maven é o mesmo de antes:

```xml
<repository>
    <id>andrei1058-snapshots</id>
    <url>https://repo.andrei1058.dev/snapshots/</url>
</repository>
<repository>
    <id>andrei1058-releases</id>
    <url>https://repo.andrei1058.dev/releases/</url>
</repository>
```

### 4. Código fonte — sem alterações

A API pública não mudou. `SidebarManager.init()`, `WrappedSidebar`, `SidebarLine`, etc. continuam nos mesmos pacotes. Nenhuma linha de código precisa ser alterada.

### 5. Build do projeto consumidor

```bash
mvn clean install -DskipTests
```

### 6. Verificação

Após o build, confira se o JAR final contém as classes NMS:

```bash
jar tf target/seu-plugin.jar | grep "ProviderImpl"
```

Deverá listar `ProviderImpl` para cada versão (v1_8_R3 até v1_21_R3).

## Rollback

Se precisar voltar, reverta as dependências no `pom.xml` para a lista antiga de módulos individuais. O `sidebar-dist` continuará disponível no repositório, mas não será mais usado.

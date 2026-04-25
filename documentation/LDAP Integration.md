
# Configuração de um LDAP de testes no OpenShift


## 📃 Introdução

O **LDAP (Lightweight Directory Access Protocol)** é um protocolo padrão utilizado para acessar e gerenciar serviços de diretório. Um diretório LDAP funciona como uma base hierárquica de dados, organizada em forma de árvore, onde são armazenadas informações como usuários, grupos, permissões e estruturas organizacionais.

Diferente de um banco de dados relacional, o LDAP é otimizado para consultas rápidas e leitura frequente, sendo amplamente utilizado em cenários de autenticação e autorização. É comum encontrar LDAP como base para sistemas de identidade corporativa, integração com aplicações, controle de acesso e centralização de credenciais.

Na prática, o LDAP pode ser utilizado para:

* Autenticação de usuários em aplicações (Single Sign-On, IAM)
* Centralização de usuários e grupos
* Controle de permissões baseado em diretório
* Integração com sistemas corporativos (APIs, portais, sistemas legados)
* Base para soluções como Keycloak, Active Directory e outros provedores de identidade


## 🎯 Objetivo deste laboratório

Neste laboratório, vamos construir um ambiente completo de LDAP dentro do OpenShift, passando por todas as etapas necessárias para seu funcionamento.

Ao longo do processo, você irá:

* Provisionar um servidor LDAP utilizando containers no OpenShift
* Configurar a estrutura inicial do diretório (backend e suffix)
* Popular o LDAP com dados utilizando arquivos LDIF
* Validar a estrutura e os dados inseridos
* Implantar uma interface web (LDAP Account Manager - LAM) para administração
* Navegar e explorar a árvore LDAP via interface gráfica

Ao final, você terá um ambiente funcional que pode ser utilizado como base para testes, estudos e integrações com outras aplicações.


## 📝 Passo a passo - Criação do LDAP Server

### 1. Criação do Namespace

Primeiro, vamos criar no OpenShift o projeto (namespace) que receberá o LDAP. Neste laboratório, utilizaremos o nome `ldap-lab`.

```bash
oc new-project ldap-lab
```

Um namespace isola os recursos do laboratório, facilitando o gerenciamento e evitando conflitos com outros ambientes.

---

### 2. Criação do Service Account e Permissões

Agora, vamos criar um *Service Account* que será utilizado pelo container do LDAP.

```bash
oc create serviceaccount dirsrv-sa -n ldap-lab
```

Em seguida, adicionamos a permissão `anyuid`, necessária porque a imagem do LDAP pode exigir execução com um usuário específico.

```bash
oc adm policy add-scc-to-user anyuid -n ldap-lab -z dirsrv-sa
```

Sem essa permissão, o pod pode falhar ao iniciar devido a restrições de segurança do OpenShift.

---

### 3. Criação da Secret com Credenciais

Vamos criar uma *Secret* para armazenar a senha do usuário administrador (Directory Manager) do LDAP.

```bash
oc create secret generic dirsrv-dm-password \
  --from-literal=dm-password='redhat123' \
  -n ldap-lab
```

O uso de secrets evita expor credenciais diretamente nos manifests YAML.

---

### 4. Deploy dos Recursos do LDAP

Agora, aplicamos os arquivos YAML responsáveis pela criação do LDAP.

```bash
cd infra/integration/ldap
```

Aplicando o StatefulSet:

```bash
oc apply -f dirsrv-statefulset.yaml
```

Aplicando o Service:

```bash
oc apply -f dirsrv-service.yaml
```

O **StatefulSet** garante identidade estável para o LDAP (necessário para serviços de diretório), enquanto o **Service** permite a comunicação interna dentro do cluster.

---

### 5. Verificação dos Recursos Criados

Após o deploy, vamos validar se o ambiente foi provisionado corretamente.

Verificando os Pods:

```bash
oc get pods
```

Exemplo de saída esperada:

```tet
NAME       READY   STATUS    RESTARTS   AGE
dirsrv-0   1/1     Running   0          4m10s
```

O status `Running` indica que o LDAP está ativo e pronto para uso.

Verificando os Services:

```bash
oc get service
```

Exemplo de saída esperada:

```text
NAME              TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)             AGE
dirsrv-internal   ClusterIP   None         <none>        3389/TCP,3636/TCP   8m57s
```

O service do tipo `ClusterIP` expõe o LDAP internamente no cluster.
As portas normalmente utilizadas são:

* 3389: LDAP (sem criptografia)
* 3636: LDAPS (LDAP sobre TLS)

---

### 6. Criação do Backend e Suffix

Agora vamos configurar a estrutura inicial do LDAP criando um **backend** e um **suffix**.

O *suffix* representa a base da árvore LDAP (equivalente a um "root" lógico), enquanto o *backend* é onde os dados serão armazenados.

```bash
oc exec -n ldap-lab -c dirsrv dirsrv-0 -- \
  dsconf localhost backend create \
  --suffix o=BANCO \
  --be-name banco \
  --create-suffix \
  --create-entries
```

> Neste caso, estamos criando uma base chamada `o=BANCO`, que será utilizada para armazenar nossos objetos LDAP.

Exemplo de saída esperada:

``` text
The database was sucessfully created
```

---

### 7. Validação da Base Criada

Após a criação do suffix, é importante validar se a estrutura foi criada corretamente.

```bash
oc exec -n ldap-lab -c dirsrv dirsrv-0 -- ldapsearch -x \
  -D "cn=Directory Manager" \
  -w 'redhat123' \
  -H ldap://localhost:3389 \
  -b "o=BANCO" \
  -s base \
  "(objectClass=*)"
```

> Esse comando realiza uma busca na base recém-criada. Se a configuração estiver correta, o retorno deverá conter informações básicas do sufixo `o=BANCO`.

Exemplo de saída esperada:

``` text
# extended LDIF
#
# LDAPv3
# base <o=BANCO> with scope baseObject
# filter: (objectClass=*)
# requesting: ALL
#

# BANCO
dn: o=BANCO
objectClass: top
objectClass: organization
o: BANCO
description: o=BANCO

# search result
search: 2
result: 0 Success

# numResponses: 2
# numEntries: 1
```

---

### 8. Importação de Dados (LDIF)

Agora vamos popular o LDAP com dados utilizando um arquivo LDIF.

Copiando o arquivo para dentro do container:

```bash
oc exec -i -n ldap-lab -c dirsrv dirsrv-0 -- sh -c 'cat > /tmp/ldap.ldif' < ldap.ldif
```

Importando os dados no LDAP:

```bash
oc exec -n ldap-lab -c dirsrv dirsrv-0 -- ldapadd -x \
  -D "cn=Directory Manager" \
  -w 'redhat123' \
  -H ldap://localhost:3389 \
  -f /tmp/ldap.ldif
```

> O comando `ldapadd` insere os registros definidos no arquivo LDIF dentro da base `o=BANCO`.

---

### 9. Teste e Validação dos Dados Importados

Após a importação, podemos realizar uma consulta para verificar se os dados foram inseridos corretamente.

```bash
oc exec -n ldap-lab -c dirsrv dirsrv-0 -- ldapsearch -x \
  -D "cn=Directory Manager" \
  -w 'redhat123' \
  -H ldap://localhost:3389 \
  -b "o=BANCO" \
  "(objectClass=*)"
```

> Esse comando lista todos os objetos armazenados no LDAP dentro do sufixo `o=BANCO`.

Exemplo de saída esperada:

``` text
...
...
# search result
search: 2
result: 0 Success

# numResponses: 28
# numEntries: 27
```

Se os registros do arquivo LDIF forem retornados, significa que o LDAP foi populado com sucesso e está pronto para uso em integrações e testes de autenticação.

---


## 📝 Passo a passo - Criação da Interface Web com LDAP Account Manager (LAM)

Após provisionar e popular o LDAP, vamos criar uma interface web para facilitar a visualização e administração dos objetos LDAP.

Neste laboratório, utilizaremos o **LDAP Account Manager (LAM)**.


### 1. Criação do Service Account do LAM

Antes de aplicar os manifests do LAM, precisamos criar um *Service Account* específico para a aplicação web.

```bash
oc create serviceaccount lam-sa -n ldap-lab
```

Em seguida, vamos atribuir a permissão `anyuid` para esse *Service Account*.

```bash
oc adm policy add-scc-to-user anyuid -n ldap-lab -z lam-sa
```

> Essa permissão é necessária porque a imagem do LAM pode precisar executar processos com um usuário específico dentro do container. Sem essa configuração, o pod pode falhar durante a inicialização por restrições de segurança do OpenShift.

---

### 2. Ajuste da Secret do LAM

Antes de aplicar a secret, é necessário ajustar o arquivo `lam-secret.yaml`, substituindo as variáveis pelos valores reais das senhas que serão utilizadas no laboratório.

O arquivo possui a seguinte estrutura:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: lam-secret
  namespace: ldap-lab
type: Opaque
stringData:
  lam-password: ${lam-password}
  ldap-bind-password: ${ldap-bind-password}
```

Substitua as seguintes variáveis por seus respectivos valores:

```text
${lam-password}
${ldap-bind-password}
```

A senha poderia ser, por exemplo: `redhat123`.

> A chave `lam-password` representa a senha de acesso à interface do LAM. A chave `ldap-bind-password` representa a senha utilizada pelo LAM para se conectar ao LDAP.

---

### 3. Aplicação dos Manifests do LAM

Após criar o *Service Account* e ajustar a secret, aplique os arquivos do LAM em ordem de precedência.

```bash
oc apply -f lam-secret.yaml
```
```bash
oc apply -f lam-config.yaml
```
```bash
oc apply -f lam-deployment.yaml
```

```bash
oc apply -f lam-service.yaml
```
```bash
oc apply -f lam-route.yaml
```

> A ordem de aplicação é importante porque o Deployment do LAM depende de recursos previamente criados, como Secret, ConfigMap e Service.

---

### 4. Validação dos Recursos do LAM

Após aplicar os manifests, verifique se os recursos foram criados corretamente.

```bash
oc get pods
```

Exemplo de saída esperada:

```text
NAME                           READY   STATUS    RESTARTS   AGE
dirsrv-0                       1/1     Running   0          20m
ldap-account-manager-xxxxx     1/1     Running   0          2m
```

Verifique também se o serviço foi criado:

```bash
oc get service
```

Exemplo de saída esperada:

```text
NAME                   TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)             AGE
dirsrv-internal        ClusterIP   None             <none>        3389/TCP,3636/TCP   6h12m
ldap-account-manager   ClusterIP   172.30.210.216   <none>        80/TCP              3h7m
```

E valide a rota de acesso ao LAM:

```bash
oc get route
```

Exemplo de saída esperada:

```text
NAME             HOST/PORT                                                                        PATH   SERVICES               PORT   TERMINATION   WILDCARD
ldap-account-manager   ldap-account-manager-ldap-lab.apps.example          ldap-account-manager   http   edge          None
```

---

### 5. Acesso à Interface Web

Com a rota criada, acesse a URL exibida no comando anterior.

A interface web do LAM poderá ser utilizada para consultar e administrar os objetos LDAP criados no laboratório.

Se o pod estiver em estado `Running` e a rota estiver acessível, a interface web do LDAP Account Manager foi provisionada com sucesso.

---

### 6. Acesso inicial ao LAM e configuração

Ao acessar a rota do LAM pela primeira vez, será exibida uma tela de configuração inicial solicitando a definição de uma nova senha.

![Tela Initial Configuration](../images/ldap/01%20-%20Initial%20Configuration.png)

Nesta etapa, defina uma senha para acesso ao LAM. Para este laboratório, podemos utilizar: `RH@123456`.

Repita a senha e clique em **Submit**.

> Essa senha será utilizada como senha principal de configuração do LAM.

---

### 7. Definição da senha master

Após o primeiro passo, será exibida uma nova tela solicitando a senha master para acessar as configurações gerais.

![Master Password Definition](../images/ldap/02%20-%20Initial%20Configuration.png)

Informe novamente a senha definida anteriormente: `RH@123456`.

Clique em **OK** para prosseguir.

---

### 8. Acesso à tela de login

Na próxima tela, será exibida a interface de login do LAM.

![LAM LDAP Login](../images/ldap/03%20-%20LAM%20LDAP%20Login.png)

Caso apareça alguma tela de configuração adicional, você pode cancelá-la. Em seguida, será redirecionado automaticamente para a tela de login.

Utilize as seguintes credenciais:

* **Usuário:** Directory Manager
* **Senha:** definida na `lam-secret.yaml` (ex: `redhat123`)

Se a autenticação for bem-sucedida, você será direcionado para o painel principal do LAM.

---

### 9. Painel principal do LAM

Após o login, você terá acesso ao painel principal do LDAP Account Manager.

![LAM Dashboard Menu](../images/ldap/04%20-%20LAM%20Tools%20Menu.png)

Nesse painel, você encontrará diversas opções para gerenciamento do LDAP, como:

* Edição de usuários e grupos
* Importação e exportação de dados
* Navegação na estrutura LDAP
* Visualização de atributos

---

### 10. Visualização da árvore LDAP (Tree View)

Para visualizar a estrutura criada no LDAP, acesse o menu superior _Tools > Tree view_.

![LAM LDAP Tree View](../images/ldap/05%20-%20LAM%20LDAP%20Tree.png)

Essa opção exibirá a árvore LDAP criada a partir do nosso arquivo LDIF, permitindo navegar pelos elementos como:

* `o=BANCO`
* `ou=APLICACOES`
* `ou=USUARIOS`
* `ou=groups`
* entre outros

Você poderá expandir os nós e visualizar os atributos de cada entrada, confirmando que os dados foram corretamente importados.

---


## 🚩 Conclusão

Ao final deste laboratório, você provisionou com sucesso um ambiente completo de LDAP dentro do OpenShift, incluindo:

* Criação do servidor LDAP utilizando StatefulSet
* Configuração de backend e suffix (`o=BANCO`)
* Importação de dados via arquivo LDIF
* Validação da estrutura e dos objetos LDAP
* Implantação da interface web com LDAP Account Manager (LAM)
* Navegação e visualização da árvore LDAP via interface gráfica

Esse ambiente pode agora ser utilizado como base para testes de autenticação, integração com aplicações e experimentação de estruturas organizacionais dentro de um diretório LDAP.


## 🪲Troubleshooting

Durante a execução do laboratório, alguns erros podem acontecer. Acompanhe na tabela abaixo os problemas mais comuns e sua possível solução.

| Erro                                     | Causa                                                                | Solução                                                                                                                                                                                                    |
| ---------------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `serviceaccount "dirsrv-sa" not found`   | Service Account não foi criada                                       | Criar a service account no namespace correto:<br><br>`oc create serviceaccount dirsrv-sa -n ldap-lab`                                                                                                      |
| `secret "dirsrv-dm-password" not found`  | Secret com a senha do LDAP não foi criada                            | Criar a secret com a senha do Directory Manager:<br><br>`oc create secret generic dirsrv-dm-password --from-literal=dm-password='redhat123' -n ldap-lab`                                                   |
| Pod não inicia (erro de permissão / SCC) | Falta da permissão `anyuid`                                          | Conceder permissão à service account:<br><br>`oc adm policy add-scc-to-user anyuid -n ldap-lab -z dirsrv-sa`                                                                                               |
| LAM não sobe ou falha no deploy          | Ordem incorreta na aplicação dos manifests ou secret mal configurada | Aplicar os arquivos na ordem correta:<br><br>`lam-secret.yaml → lam-config.yaml → lam-service.yaml → lam-deployment.yaml → lam-route.yaml`<br><br>Validar também se as senhas foram substituídas na secret |
| Falha de login no LAM                    | Credenciais incorretas ou LDAP não acessível                         | Verificar usuário (`Directory Manager`) e senha definida na secret. Validar conectividade com LDAP                                                                                                         |                                                                                                |
| Dados não aparecem no Tree View          | LDIF não foi importado corretamente                                  | Validar conteúdo do LDAP:<br><br>`oc exec -n ldap-lab -c dirsrv dirsrv-0 -- ldapsearch -x -D "cn=Directory Manager" -w 'redhat123' -H ldap://localhost:3389 -b "o=BANCO" "(objectClass=*)"`                |
| Erro ao importar LDIF                    | Arquivo não foi copiado corretamente para o container                | Reexecutar cópia do arquivo:<br><br>`oc exec -i -n ldap-lab -c dirsrv dirsrv-0 -- sh -c 'cat > /tmp/ldap.ldif' < ldap.ldif`                                                                                |

---


## 📖 Links para Referência

- [What is lightweight directory access protocol (LDAP) authentication?](https://www.redhat.com/en/topics/security/what-is-ldap-authentication)
- [Easy LDAP Management](https://www.ldap-account-manager.org/lamcms/)

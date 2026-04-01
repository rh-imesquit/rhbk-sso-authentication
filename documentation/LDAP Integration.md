oc create serviceaccount dirsrv-sa -n ldap-lab
oc adm policy add-scc-to-user anyuid -n ldap-lab -z dirsrv-sa

oc create secret generic dirsrv-dm-password \
  --from-literal=dm-password='redhat123' \
  -n ldap-lab



## Criar backend e suffix

oc exec -n ldap-lab -c dirsrv dirsrv-0 -- \
  dsconf localhost backend create \
  --suffix o=BANCO \
  --be-name banco \
  --create-suffix \
  --create-entries

## Validar que a base existe

oc exec -n ldap-lab -c dirsrv dirsrv-0 -- ldapsearch -x \
  -D "cn=Directory Manager" \
  -w 'redhat123' \
  -H ldap://localhost:3389 \
  -b "o=BANCO" \
  -s base \
  "(objectClass=*)"

## Importar o arquivo ldif

oc exec -i -n ldap-lab -c dirsrv dirsrv-0 -- sh -c 'cat > /tmp/ldap.ldif' < ldap.ldif


oc exec -n ldap-lab -c dirsrv dirsrv-0 -- ldapadd -x \
  -D "cn=Directory Manager" \
  -w 'redhat123' \
  -H ldap://localhost:3389 \
  -f /tmp/ldap.ldif


oc exec -n ldap-lab -c dirsrv dirsrv-0 -- ldapsearch -x \
  -D "cn=Directory Manager" \
  -w 'redhat123' \
  -H ldap://localhost:3389 \
  -b "o=BANCO" \
  "(objectClass=*)"



# Criando service account para o LAM

oc create serviceaccount lam-sa -n ldap-lab
oc adm policy add-scc-to-user anyuid -n ldap-lab -z lam-sa


# lam-secret.yaml

${lam-password}
${ldap-bind-password}
import os, io

file_path = r'C:\Users\INFOKOM\Desktop\RH-APPLICATION-FRONTEND\src\app\layout\common\notifications\notifications.service.ts'

with io.open(file_path, 'r', encoding='utf8') as f:
    content = f.read()

content = content.replace("const NOTIF_API = '/api/transport-notifications';", "const NOTIF_API = '/api/transport-notifications';\nconst MUTUELLE_API = '/api/mutuelle-notifications';")

# Update getAll
old_get_all = '''        try {
            const localUser = JSON.parse(localUserStr);
            const user$ = this._httpClient.get<any[]>(`${NOTIF_API}/destinataire/${localUser.id}/non-lues`).pipe(
                catchError((error) => {
                    console.error('Chargement notifications utilisateur', error);
                    return of([]);
                }),
                map(rows => (rows || []).map(bn => this.mapBackendRow(bn)))
            );
            const admin$ = localUser.role === 'ADMIN'
                ? this._httpClient.get<any[]>(`${NOTIF_API}/destinataire/ADMIN/non-lues`).pipe(
                    catchError(() => of([])),
                    map(rows => (rows || []).map(bn => this.mapBackendRow(bn)))
                )
                : of([] as Notification[]);

            return forkJoin({ user: user$, admin: admin$ }).pipe(
                map(({ user, admin }) => this.mergeById(user, admin)),'''

new_get_all = '''        try {
            const localUser = JSON.parse(localUserStr);
            
            const mutuelle$ = this._httpClient.get<any[]>(`${MUTUELLE_API}`).pipe(
                catchError(() => of([])),
                map(rows => (rows || []).map(bn => this.mapBackendRow(bn)))
            );
            
            const user$ = this._httpClient.get<any[]>(`${NOTIF_API}/destinataire/${localUser.id}/non-lues`).pipe(
                catchError((error) => {
                    console.error('Chargement notifications utilisateur', error);
                    return of([]);
                }),
                map(rows => (rows || []).map(bn => this.mapBackendRow(bn)))
            );
            const admin$ = localUser.role === 'ADMIN'
                ? this._httpClient.get<any[]>(`${NOTIF_API}/destinataire/ADMIN/non-lues`).pipe(
                    catchError(() => of([])),
                    map(rows => (rows || []).map(bn => this.mapBackendRow(bn)))
                )
                : of([] as Notification[]);

            return forkJoin({ mutuelle: mutuelle$, user: user$, admin: admin$ }).pipe(
                map(({ mutuelle, user, admin }) => this.mergeById(this.mergeById(mutuelle, user), admin)),'''

content = content.replace(old_get_all, new_get_all)

# Fix mapBackendRow to handle Mutuelle fields properly
old_map = '''        const n: Notification = {
            id: bn.id,
            icon,
            title: bn.titreOffreAvantage || title,
            description: bn.contenu || bn.message || 'Nouvelle notification système',
            time: bn.dateCreation || new Date().toISOString(),
            read: bn.lu || false,
            type: t,
            reservationId: bn.reservationId,
            trajetId: bn.trajetId,
            trajetAnnuleId: bn.trajetAnnuleId || bn.trajetId,
            expediteurId: bn.expediteurId,
            destinataireId: bn.destinataireId,
            contenu: bn.contenu
        };'''

new_map = '''        const n: Notification = {
            id: bn.id,
            icon,
            title: bn.titreOffreAvantage || title,
            description: bn.contenu || bn.message || 'Nouvelle notification système',
            time: bn.dateCreation || new Date().toISOString(),
            read: bn.lu || false,
            type: t,
            reservationId: bn.reservationId,
            trajetId: bn.trajetId,
            trajetAnnuleId: bn.trajetAnnuleId || bn.trajetId,
            expediteurId: bn.expediteurId,
            destinataireId: bn.destinataireId,
            contenu: bn.contenu,
            idUser: bn.idUser // Add idUser to identify mutuelle
        };'''

content = content.replace(old_map, new_map)

# Update the 'update' method
old_update = '''                if (notification.type) {
                    return this._httpClient.put<Notification>(`${NOTIF_API}/${id}/lire`, {}).pipe('''

new_update = '''                const isMutuelle = !!(notification as any)['idUser'];
                const endpoint = isMutuelle ? `${MUTUELLE_API}/${id}/lire` : `${NOTIF_API}/${id}/lire`;
                const request$ = isMutuelle ? this._httpClient.patch<Notification>(endpoint, {}) : this._httpClient.put<Notification>(endpoint, {});
                
                if (notification.type || isMutuelle) {
                    return request$.pipe('''

content = content.replace(old_update, new_update)

# Update the 'delete' method
old_delete = '''                if (targetNode?.type) {
                    request$ = this._httpClient.delete<boolean>(`${NOTIF_API}/${id}`).pipe('''

new_delete = '''                if (targetNode) {
                    const isMutuelle = !!(targetNode as any)['idUser'];
                    const endpoint = isMutuelle ? `${MUTUELLE_API}/${id}` : `${NOTIF_API}/${id}`;
                    request$ = this._httpClient.delete<boolean>(endpoint).pipe('''

content = content.replace(old_delete, new_delete)

with io.open(file_path, 'w', encoding='utf8') as f:
    f.write(content)

print('Updated frontend notifications.service.ts successfully!')

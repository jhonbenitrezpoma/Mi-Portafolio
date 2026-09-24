-- =========================================================
-- UPLA PORTAFOLIO
-- CONFIGURACIÓN DE SUPABASE
-- =========================================================


-- =========================================================
-- EXTENSIÓN PARA UUID
-- =========================================================

create extension if not exists "pgcrypto";


-- =========================================================
-- TABLA: AJUSTES
-- =========================================================

create table if not exists public.ajustes (

    id uuid primary key default gen_random_uuid(),

    nombre text,

    valor text,

    created_at timestamptz
        default now(),

    updated_at timestamptz
        default now()

);


-- =========================================================
-- TABLA: TEMAS / CURSOS
-- =========================================================

create table if not exists public.temas (

    id uuid primary key
        default gen_random_uuid(),

    numero text,

    nombre text not null,

    descripcion text,

    imagen text,

    created_at timestamptz
        default now(),

    updated_at timestamptz
        default now()

);


-- =========================================================
-- TABLA: TAREAS
-- =========================================================

create table if not exists public.tareas (

    id uuid primary key
        default gen_random_uuid(),

    tema_id uuid
        references public.temas(id)
        on delete cascade,

    titulo text not null,

    descripcion text,

    url text,

    unidad integer
        not null
        default 1,

    created_at timestamptz
        default now(),

    updated_at timestamptz
        default now()

);


-- =========================================================
-- TABLA: COMENTARIOS
-- =========================================================

create table if not exists public.comentarios (

    id uuid primary key
        default gen_random_uuid(),

    usuario_id uuid
        references auth.users(id)
        on delete cascade,

    nombre text,

    contenido text not null,

    created_at timestamptz
        default now()

);


-- =========================================================
-- ACTIVAR RLS
-- =========================================================

alter table public.ajustes
enable row level security;

alter table public.temas
enable row level security;

alter table public.tareas
enable row level security;

alter table public.comentarios
enable row level security;


-- =========================================================
-- ELIMINAR POLÍTICAS ANTERIORES
-- =========================================================

drop policy if exists
"Lectura pública ajustes"
on public.ajustes;

drop policy if exists
"Lectura pública temas"
on public.temas;

drop policy if exists
"Lectura pública tareas"
on public.tareas;

drop policy if exists
"Lectura pública comentarios"
on public.comentarios;

drop policy if exists
"Usuarios pueden comentar"
on public.comentarios;

drop policy if exists
"Usuarios pueden eliminar sus comentarios"
on public.comentarios;

drop policy if exists
"Usuarios pueden actualizar sus comentarios"
on public.comentarios;

drop policy if exists
"Usuarios autenticados pueden crear tareas"
on public.tareas;

drop policy if exists
"Usuarios autenticados pueden actualizar tareas"
on public.tareas;

drop policy if exists
"Usuarios autenticados pueden eliminar tareas"
on public.tareas;


-- =========================================================
-- LECTURA PÚBLICA DE AJUSTES
-- =========================================================

create policy
"Lectura pública ajustes"

on public.ajustes

for select

to anon, authenticated

using (true);


-- =========================================================
-- LECTURA PÚBLICA DE CURSOS
-- =========================================================

create policy
"Lectura pública temas"

on public.temas

for select

to anon, authenticated

using (true);


-- =========================================================
-- LECTURA PÚBLICA DE TAREAS
-- =========================================================

create policy
"Lectura pública tareas"

on public.tareas

for select

to anon, authenticated

using (true);


-- =========================================================
-- LECTURA PÚBLICA DE COMENTARIOS
-- =========================================================

create policy
"Lectura pública comentarios"

on public.comentarios

for select

to anon, authenticated

using (true);


-- =========================================================
-- COMENTARIOS
-- =========================================================

create policy
"Usuarios pueden comentar"

on public.comentarios

for insert

to authenticated

with check (
    auth.uid() = usuario_id
);


create policy
"Usuarios pueden eliminar sus comentarios"

on public.comentarios

for delete

to authenticated

using (
    auth.uid() = usuario_id
);


create policy
"Usuarios pueden actualizar sus comentarios"

on public.comentarios

for update

to authenticated

using (
    auth.uid() = usuario_id
)

with check (
    auth.uid() = usuario_id
);


-- =========================================================
-- TAREAS
-- =========================================================

create policy
"Usuarios autenticados pueden crear tareas"

on public.tareas

for insert

to authenticated

with check (true);


create policy
"Usuarios autenticados pueden actualizar tareas"

on public.tareas

for update

to authenticated

using (true)

with check (true);


create policy
"Usuarios autenticados pueden eliminar tareas"

on public.tareas

for delete

to authenticated

using (true);


-- =========================================================
-- DATOS INICIALES
-- =========================================================

insert into public.temas
(
    numero,
    nombre,
    descripcion
)

select
    '01',
    'Desarrollo de Aplicaciones 1',
    'Curso orientado al desarrollo de aplicaciones y programación.'

where not exists (

    select 1
    from public.temas
    where nombre =
        'Desarrollo de Aplicaciones 1'

);


insert into public.temas
(
    numero,
    nombre,
    descripcion
)

select
    '02',
    'Algoritmos y Estructura de Datos',
    'Estudio de algoritmos, estructuras de datos y resolución de problemas.'

where not exists (

    select 1
    from public.temas
    where nombre =
        'Algoritmos y Estructura de Datos'

);


-- =========================================================
-- STORAGE
-- =========================================================

insert into storage.buckets
(
    id,
    name,
    public
)

values
(
    'portafolio',
    'portafolio',
    true
)

on conflict (id)
do update set
    public = true;


-- =========================================================
-- POLÍTICAS DEL STORAGE
-- =========================================================

drop policy if exists
"Imágenes públicas portafolio"
on storage.objects;


drop policy if exists
"Usuarios pueden subir imágenes portafolio"
on storage.objects;


drop policy if exists
"Usuarios pueden actualizar imágenes portafolio"
on storage.objects;


drop policy if exists
"Usuarios pueden eliminar imágenes portafolio"
on storage.objects;


create policy
"Imágenes públicas portafolio"

on storage.objects

for select

to public

using (
    bucket_id = 'portafolio'
);


create policy
"Usuarios pueden subir imágenes portafolio"

on storage.objects

for insert

to authenticated

with check (
    bucket_id = 'portafolio'
);


create policy
"Usuarios pueden actualizar imágenes portafolio"

on storage.objects

for update

to authenticated

using (
    bucket_id = 'portafolio'
)

with check (
    bucket_id = 'portafolio'
);


create policy
"Usuarios pueden eliminar imágenes portafolio"

on storage.objects

for delete

to authenticated

using (
    bucket_id = 'portafolio'
);


-- =========================================================
-- FIN
-- =========================================================

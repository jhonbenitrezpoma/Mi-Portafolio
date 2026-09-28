-- =====================================================================
--  PORTAFOLIO UPLA - Configuración de base de datos (Supabase)
--  Pega TODO este archivo en: Supabase > SQL Editor > New query > Run
--  IMPORTANTE: cambia 'TU_CORREO_AQUI' (línea marcada) por el correo
--  con el que crearás tu usuario propietario en Authentication > Users.
-- =====================================================================

-- 1) TABLAS ------------------------------------------------------------
create table if not exists public.ajustes (
  clave text primary key,            -- 'logo' | 'foto_perfil'
  valor text,                        -- URL pública de la imagen
  updated_at timestamptz default now()
);

create table if not exists public.tareas (
  id uuid primary key default gen_random_uuid(),
  clave text not null,               -- ej: curso1_U1_S1
  nombre text not null,
  url text not null,
  tipo text not null check (tipo in ('archivo', 'enlace')),
  creado_en timestamptz default now()
);

create table if not exists public.temas (
  clave text primary key,            -- ej: curso1_U1_S1
  titulo text
);

create table if not exists public.comentarios (
  id uuid primary key default gen_random_uuid(),
  nombre text not null check (char_length(nombre) between 1 and 60),
  mensaje text not null check (char_length(mensaje) between 1 and 500),
  creado_en timestamptz default now()
);

-- 2) FUNCIÓN: ¿quién es el propietario? --------------------------------
--    >>> CAMBIA EL CORREO AQUÍ <<<
create or replace function public.es_propietario()
returns boolean
language sql
stable
as $$
  select (auth.jwt() ->> 'email') = 'tuhermana1591q@gmail.com'
$$;

-- 3) SEGURIDAD (RLS): todos leen, solo el propietario escribe ----------
alter table public.ajustes     enable row level security;
alter table public.tareas      enable row level security;
alter table public.temas       enable row level security;
alter table public.comentarios enable row level security;

create policy "todos ven ajustes"     on public.ajustes     for select using (true);
create policy "todos ven tareas"      on public.tareas      for select using (true);
create policy "todos ven temas"       on public.temas       for select using (true);
create policy "todos ven comentarios" on public.comentarios for select using (true);

create policy "propietario gestiona ajustes" on public.ajustes
  for all using (public.es_propietario()) with check (public.es_propietario());
create policy "propietario gestiona tareas" on public.tareas
  for all using (public.es_propietario()) with check (public.es_propietario());
create policy "propietario gestiona temas" on public.temas
  for all using (public.es_propietario()) with check (public.es_propietario());

-- Los visitantes pueden dejar sugerencias; solo el propietario las borra
create policy "todos pueden comentar" on public.comentarios
  for insert to anon, authenticated with check (true);
create policy "propietario borra comentarios" on public.comentarios
  for delete using (public.es_propietario());

-- 4) ALMACENAMIENTO (imágenes y archivos de tareas) --------------------
insert into storage.buckets (id, name, public)
values ('portafolio', 'portafolio', true)
on conflict (id) do nothing;

create policy "todos ven archivos" on storage.objects
  for select using (bucket_id = 'portafolio');
create policy "propietario sube archivos" on storage.objects
  for insert with check (bucket_id = 'portafolio' and public.es_propietario());
create policy "propietario edita archivos" on storage.objects
  for update using (bucket_id = 'portafolio' and public.es_propietario());
create policy "propietario borra archivos" on storage.objects
  for delete using (bucket_id = 'portafolio' and public.es_propietario());

-- 5) TIEMPO REAL (para que los visitantes vean los cambios al instante) -
alter publication supabase_realtime add table
  public.ajustes, public.tareas, public.temas, public.comentarios;

-- =========================================================
-- FIN
-- =========================================================

--
-- PostgreSQL database dump
--

-- Dumped from database version 15.10
-- Dumped by pg_dump version 16.6
--
-- Started on 2025-10-22 12:23:11
-- Base de données : ventes_dakar pour les ventes de Dakar, ventes_thies pour les ventes de Thies, ventes_stl pour les ventes de Saint-Louis 
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 214 (class 1259 OID 81935)
-- Name: vente; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.vente (
    id uuid NOT NULL,
    date_vente date,
    montant double precision,
    produit character varying(255),
    region character varying(100),
    updated_at timestamp without time zone,
    deleted boolean,
    deleted_at timestamp without time zone
);


ALTER TABLE public.vente OWNER TO postgres;

--
-- TOC entry 3173 (class 2606 OID 81939)
-- Name: vente vente_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.vente
    ADD CONSTRAINT vente_pkey PRIMARY KEY (id);


--
-- TOC entry 3321 (class 0 OID 0)
-- Dependencies: 214
-- Name: TABLE vente; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.vente TO dsms_user;


-- Completed on 2025-10-22 12:23:11

--
-- PostgreSQL database dump complete
--


--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: clagg_reset(); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION clagg_reset() RETURNS void
    LANGUAGE plpgsql
    AS $$BEGIN
	PERFORM SETVAL('seq_areas_id', 0);
	PERFORM SETVAL('seq_jobs_id', 0);
	PERFORM SETVAL('seq_listings_id', 0);
	PERFORM SETVAL('seq_locations_id', 0);
	PERFORM SETVAL('seq_sections_id', 0);

	TRUNCATE listings CASCADE;
	TRUNCATE jobs CASCADE;
	TRUNCATE locations CASCADE;
END;$$;


ALTER FUNCTION public.clagg_reset() OWNER TO clagg;

--
-- Name: job_create(smallint, refcursor); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION job_create(i_job_type smallint, c_new_job refcursor) RETURNS TABLE(new_job refcursor)
    LANGUAGE plpgsql
    AS $_$
DECLARE
	new_id bigint;
	job_type ALIAS FOR $1;
	new_job ALIAS FOR $2;
BEGIN

	SELECT NEXTVAL('seq_jobs_id') INTO new_id;

	INSERT INTO jobs (id, started_on, type) VALUES (new_id, CURRENT_TIMESTAMP, job_type);

	OPEN new_job FOR SELECT * FROM jobs WHERE id = new_id;

	RETURN QUERY SELECT new_job AS new_job;
END
$_$;


ALTER FUNCTION public.job_create(i_job_type smallint, c_new_job refcursor) OWNER TO clagg;

--
-- Name: FUNCTION job_create(i_job_type smallint, c_new_job refcursor); Type: COMMENT; Schema: public; Owner: clagg
--

COMMENT ON FUNCTION job_create(i_job_type smallint, c_new_job refcursor) IS 'Creates a new job definition and returns it to the caller.';


--
-- Name: job_finish(bigint); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION job_finish(i_job_id bigint) RETURNS void
    LANGUAGE sql
    AS $$
    UPDATE jobs SET ended_on = CURRENT_TIMESTAMP WHERE id = i_job_id
  $$;


ALTER FUNCTION public.job_finish(i_job_id bigint) OWNER TO clagg;

--
-- Name: listing_create(bigint, character varying, character varying, integer, numeric, numeric, character varying); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION listing_create(i_job_id bigint, i_url character varying, i_title character varying, i_price integer, i_lat numeric, i_long numeric, i_address character varying) RETURNS TABLE(new_listing refcursor, listing_location refcursor)
    LANGUAGE plpgsql
    AS $$DECLARE
	list_id bigint;
	location_count integer;
	location_id bigint;
	new_listing refcursor;
	listing_location refcursor;
BEGIN
	-- first, determine the location id, to see if we should reuse an existing one
	SELECT COUNT(id) INTO location_count FROM locations WHERE (lat = i_lat AND long = i_long);

	IF location_count = 0 THEN
		SELECT NEXTVAL('seq_locations_id') INTO location_id;
		INSERT INTO locations(id, lat, long, address)
			VALUES(location_id, i_lat, i_long, i_address);
	ELSE
		SELECT id INTO location_id FROM locations WHERE lat = i_lat AND long = i_long;
	END IF;

	-- then insert the listing
	SELECT NEXTVAL('seq_listings_id') INTO list_id;
	
	INSERT INTO listings(id, url, added_on, job_id, location_id, title, price) 
		VALUES (list_id, i_url, CURRENT_TIMESTAMP, i_job_id, location_id, i_title, i_price);

	OPEN new_listing FOR SELECT * FROM listings WHERE id = list_id;

	OPEN listing_location FOR SELECT * FROM locations WHERE id = location_id;

	RETURN QUERY SELECT new_listing, listing_location;
END;$$;


ALTER FUNCTION public.listing_create(i_job_id bigint, i_url character varying, i_title character varying, i_price integer, i_lat numeric, i_long numeric, i_address character varying) OWNER TO clagg;

--
-- Name: listing_delete(bigint[]); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION listing_delete(i_job_ids bigint[]) RETURNS void
    LANGUAGE plpgsql
    AS $$DECLARE
	i_id bigint;
BEGIN

	FOREACH i_id IN ARRAY i_job_ids
	LOOP
		UPDATE listings SET deleted_on = CURRENT_TIMESTAMP WHERE id = i_id;
	END LOOP;

END;$$;


ALTER FUNCTION public.listing_delete(i_job_ids bigint[]) OWNER TO clagg;

--
-- Name: listing_exists_for_url(character varying); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION listing_exists_for_url(i_url character varying) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
DECLARE
	listing_exists boolean = FALSE;
	listing_count bigint;
BEGIN
	SELECT COUNT(id) INTO listing_count FROM listings WHERE url = i_url;
	
	RETURN listing_count > 0;
END
$$;


ALTER FUNCTION public.listing_exists_for_url(i_url character varying) OWNER TO clagg;

--
-- Name: listing_get_all(boolean, refcursor, bigint, bigint); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION listing_get_all(include_deleted boolean, c_listings refcursor, page_size bigint, page_number bigint) RETURNS SETOF refcursor
    LANGUAGE plpgsql
    AS $_$DECLARE
	listings ALIAS FOR $2;
BEGIN
	-- added_on is indexed descending, since we usually want to validate more recent entries, so use that to improve page performance
	IF include_deleted = TRUE THEN
		OPEN listings FOR SELECT * FROM listings 
				ORDER BY added_on desc 
				OFFSET page_number * page_size 
				LIMIT page_size;
	ELSE
		OPEN listings FOR SELECT * FROM listings 
				WHERE deleted_on IS NULL 
				ORDER BY added_on DESC 
				OFFSET page_number * page_size 
				LIMIT page_size;
	END IF;

	RETURN QUERY SELECT listings AS listings;
END;$_$;


ALTER FUNCTION public.listing_get_all(include_deleted boolean, c_listings refcursor, page_size bigint, page_number bigint) OWNER TO clagg;

--
-- Name: listing_get_count(boolean); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION listing_get_count(include_deleted boolean) RETURNS bigint
    LANGUAGE plpgsql
    AS $$
DECLARE
	listing_count bigint;
BEGIN
	IF include_deleted = TRUE THEN
		SELECT COUNT(*) INTO listing_count FROM listings;
	ELSE
		SELECT COUNT(*) INTO listing_count FROM listings WHERE deleted_on IS NULL;
	END IF;

	RETURN listing_count;
END$$;


ALTER FUNCTION public.listing_get_count(include_deleted boolean) OWNER TO clagg;

--
-- Name: listing_get_unvalidated(boolean, refcursor, bigint, bigint); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION listing_get_unvalidated(include_deleted boolean, c_listings refcursor, page_size bigint, page_number bigint) RETURNS SETOF refcursor
    LANGUAGE plpgsql
    AS $_$DECLARE
	listings ALIAS FOR $2;
	start_date timestamp with time zone;
BEGIN
	-- unvalidated listings are those which have been added since the last validation job finished

	SELECT MAX(ended_on) INTO start_date FROM jobs WHERE type = 2 AND ended_on IS NOT NULL;	-- FIXME should this type be hardcoded?
	
	-- added_on is indexed descending, since we usually want to validate more recent entries, so use that to improve paging performance
	IF include_deleted = TRUE THEN
		OPEN listings FOR SELECT * FROM listings 
				WHERE added_on >= start_date
				ORDER BY added_on desc
				OFFSET page_number * page_size
				LIMIT page_size;
	ELSE
		OPEN listings FOR SELECT * FROM listings 
				WHERE deleted_on IS NULL 
					AND added_on >= start_date
				ORDER BY added_on DESC 
				OFFSET page_number * page_size 
				LIMIT page_size;
	END IF;

	RETURN QUERY SELECT listings AS listings;
END;$_$;


ALTER FUNCTION public.listing_get_unvalidated(include_deleted boolean, c_listings refcursor, page_size bigint, page_number bigint) OWNER TO clagg;

--
-- Name: listing_get_unvalidated_count(boolean); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION listing_get_unvalidated_count(include_deleted boolean) RETURNS bigint
    LANGUAGE plpgsql
    AS $$
DECLARE
	listing_count bigint;
	start_date timestamp with time zone;
BEGIN
	-- unvalidated listings are those which have been added since the last validation job finished

	SELECT MAX(ended_on) INTO start_date FROM jobs WHERE type = 2 AND ended_on IS NOT NULL;	-- FIXME should this type be hardcoded?

	IF include_deleted = TRUE THEN
		SELECT COUNT(*) INTO listing_count FROM listings WHERE added_on >= start_date;
	ELSE
		SELECT COUNT(*) INTO listing_count FROM listings WHERE deleted_on IS NULL AND added_on >= start_date;
	END IF;

	RETURN listing_count;
END$$;


ALTER FUNCTION public.listing_get_unvalidated_count(include_deleted boolean) OWNER TO clagg;

--
-- Name: query_get_all(refcursor); Type: FUNCTION; Schema: public; Owner: clagg
--

CREATE FUNCTION query_get_all(c_queries refcursor) RETURNS SETOF refcursor
    LANGUAGE plpgsql
    AS $_$
DECLARE
	queries ALIAS FOR $1;
BEGIN
	OPEN queries FOR SELECT * FROM queries;

	RETURN QUERY SELECT queries AS queries;
END
$_$;


ALTER FUNCTION public.query_get_all(c_queries refcursor) OWNER TO clagg;

--
-- Name: FUNCTION query_get_all(c_queries refcursor); Type: COMMENT; Schema: public; Owner: clagg
--

COMMENT ON FUNCTION query_get_all(c_queries refcursor) IS 'Gets all active query URLs.';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: areas; Type: TABLE; Schema: public; Owner: clagg; Tablespace: 
--

CREATE TABLE areas (
    id bigint NOT NULL,
    parent_area_id bigint,
    code character varying(6) COLLATE pg_catalog."en_US.utf8" NOT NULL,
    description character varying(1000) COLLATE pg_catalog."en_US.utf8" NOT NULL
);


ALTER TABLE public.areas OWNER TO clagg;

--
-- Name: TABLE areas; Type: COMMENT; Schema: public; Owner: clagg
--

COMMENT ON TABLE areas IS 'Represents geographic areas';


--
-- Name: jobs; Type: TABLE; Schema: public; Owner: clagg; Tablespace: 
--

CREATE TABLE jobs (
    id bigint NOT NULL,
    started_on timestamp with time zone NOT NULL,
    ended_on timestamp with time zone,
    type smallint NOT NULL,
    CONSTRAINT "CK_job_type" CHECK ((type = ANY (ARRAY[1, 2])))
);


ALTER TABLE public.jobs OWNER TO clagg;

--
-- Name: listings; Type: TABLE; Schema: public; Owner: clagg; Tablespace: 
--

CREATE TABLE listings (
    listing_id numeric(30,0),
    url character varying(1000) NOT NULL,
    id bigint NOT NULL,
    last_modified_on timestamp with time zone,
    deleted_on timestamp with time zone,
    added_on timestamp with time zone NOT NULL,
    job_id bigint NOT NULL,
    section_id bigint,
    location_id bigint,
    area_id bigint,
    title character varying(1000),
    ignore boolean DEFAULT false,
    price integer NOT NULL,
    query_id bigint
);


ALTER TABLE public.listings OWNER TO clagg;

--
-- Name: locations; Type: TABLE; Schema: public; Owner: clagg; Tablespace: 
--

CREATE TABLE locations (
    id bigint NOT NULL,
    lat numeric(15,12),
    long numeric(15,12),
    address character varying(8000)
);


ALTER TABLE public.locations OWNER TO clagg;

--
-- Name: queries; Type: TABLE; Schema: public; Owner: clagg; Tablespace: 
--

CREATE TABLE queries (
    id bigint NOT NULL,
    site_name character varying(32) NOT NULL,
    url character varying(2048) NOT NULL,
    page_step smallint NOT NULL,
    page_size smallint NOT NULL,
    max_pages smallint NOT NULL
);


ALTER TABLE public.queries OWNER TO clagg;

--
-- Name: queries_id_seq; Type: SEQUENCE; Schema: public; Owner: clagg
--

CREATE SEQUENCE queries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.queries_id_seq OWNER TO clagg;

--
-- Name: queries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: clagg
--

ALTER SEQUENCE queries_id_seq OWNED BY queries.id;


--
-- Name: sections; Type: TABLE; Schema: public; Owner: clagg; Tablespace: 
--

CREATE TABLE sections (
    id bigint NOT NULL,
    code character varying(6) COLLATE pg_catalog."en_US.utf8" NOT NULL,
    description character varying(1000) COLLATE pg_catalog."en_US.utf8" NOT NULL
);


ALTER TABLE public.sections OWNER TO clagg;

--
-- Name: TABLE sections; Type: COMMENT; Schema: public; Owner: clagg
--

COMMENT ON TABLE sections IS 'Represents listing categories';


--
-- Name: seq_areas_id; Type: SEQUENCE; Schema: public; Owner: clagg
--

CREATE SEQUENCE seq_areas_id
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_areas_id OWNER TO clagg;

--
-- Name: seq_jobs_id; Type: SEQUENCE; Schema: public; Owner: clagg
--

CREATE SEQUENCE seq_jobs_id
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_jobs_id OWNER TO clagg;

--
-- Name: seq_listings_id; Type: SEQUENCE; Schema: public; Owner: clagg
--

CREATE SEQUENCE seq_listings_id
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_listings_id OWNER TO clagg;

--
-- Name: seq_locations_id; Type: SEQUENCE; Schema: public; Owner: clagg
--

CREATE SEQUENCE seq_locations_id
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_locations_id OWNER TO clagg;

--
-- Name: seq_sections_id; Type: SEQUENCE; Schema: public; Owner: clagg
--

CREATE SEQUENCE seq_sections_id
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.seq_sections_id OWNER TO clagg;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: clagg
--

ALTER TABLE ONLY queries ALTER COLUMN id SET DEFAULT nextval('queries_id_seq'::regclass);


--
-- Name: PK_areas; Type: CONSTRAINT; Schema: public; Owner: clagg; Tablespace: 
--

ALTER TABLE ONLY areas
    ADD CONSTRAINT "PK_areas" PRIMARY KEY (id);


--
-- Name: PK_jobs; Type: CONSTRAINT; Schema: public; Owner: clagg; Tablespace: 
--

ALTER TABLE ONLY jobs
    ADD CONSTRAINT "PK_jobs" PRIMARY KEY (id);


--
-- Name: PK_listings; Type: CONSTRAINT; Schema: public; Owner: clagg; Tablespace: 
--

ALTER TABLE ONLY listings
    ADD CONSTRAINT "PK_listings" PRIMARY KEY (id);


--
-- Name: PK_locations; Type: CONSTRAINT; Schema: public; Owner: clagg; Tablespace: 
--

ALTER TABLE ONLY locations
    ADD CONSTRAINT "PK_locations" PRIMARY KEY (id);


--
-- Name: PK_sections; Type: CONSTRAINT; Schema: public; Owner: clagg; Tablespace: 
--

ALTER TABLE ONLY sections
    ADD CONSTRAINT "PK_sections" PRIMARY KEY (id);


--
-- Name: pk_queries; Type: CONSTRAINT; Schema: public; Owner: clagg; Tablespace: 
--

ALTER TABLE ONLY queries
    ADD CONSTRAINT pk_queries PRIMARY KEY (id);


--
-- Name: FKI_areas_parent; Type: INDEX; Schema: public; Owner: clagg; Tablespace: 
--

CREATE INDEX "FKI_areas_parent" ON areas USING btree (parent_area_id);


--
-- Name: FKI_listings_areas; Type: INDEX; Schema: public; Owner: clagg; Tablespace: 
--

CREATE INDEX "FKI_listings_areas" ON listings USING btree (area_id);


--
-- Name: FKI_listings_jobs; Type: INDEX; Schema: public; Owner: clagg; Tablespace: 
--

CREATE INDEX "FKI_listings_jobs" ON listings USING btree (job_id);


--
-- Name: IX_listings_added_on; Type: INDEX; Schema: public; Owner: clagg; Tablespace: 
--

CREATE INDEX "IX_listings_added_on" ON listings USING btree (added_on DESC NULLS LAST);


--
-- Name: IX_listings_deleted_on; Type: INDEX; Schema: public; Owner: clagg; Tablespace: 
--

CREATE INDEX "IX_listings_deleted_on" ON listings USING btree (deleted_on DESC NULLS LAST);


--
-- Name: FK_areas_parent; Type: FK CONSTRAINT; Schema: public; Owner: clagg
--

ALTER TABLE ONLY areas
    ADD CONSTRAINT "FK_areas_parent" FOREIGN KEY (parent_area_id) REFERENCES areas(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: FK_listings_areas; Type: FK CONSTRAINT; Schema: public; Owner: clagg
--

ALTER TABLE ONLY listings
    ADD CONSTRAINT "FK_listings_areas" FOREIGN KEY (area_id) REFERENCES areas(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: FK_listings_jobs; Type: FK CONSTRAINT; Schema: public; Owner: clagg
--

ALTER TABLE ONLY listings
    ADD CONSTRAINT "FK_listings_jobs" FOREIGN KEY (job_id) REFERENCES jobs(id) ON UPDATE SET NULL ON DELETE SET NULL;


--
-- Name: FK_listings_locations; Type: FK CONSTRAINT; Schema: public; Owner: clagg
--

ALTER TABLE ONLY listings
    ADD CONSTRAINT "FK_listings_locations" FOREIGN KEY (location_id) REFERENCES locations(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: FK_listings_sections; Type: FK CONSTRAINT; Schema: public; Owner: clagg
--

ALTER TABLE ONLY listings
    ADD CONSTRAINT "FK_listings_sections" FOREIGN KEY (section_id) REFERENCES sections(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--


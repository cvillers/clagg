using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using Npgsql;
using NpgsqlTypes;

namespace Downloader
{
	enum JobTypes : short
	{
		Crawl = 1,
		Validate = 2,
		Download = 3
	}

	class DbConnection
	{

		private NpgsqlConnection GetConnection()
		{
			var connection = new NpgsqlConnection(ConfigurationManager.ConnectionStrings["ClaggDB"].ConnectionString);

			connection.Open();

			return connection;
		}

		public long CreateJob(JobTypes type)
		{
			long ret = 0;

			var connection = GetConnection();

			var tx = connection.BeginTransaction();
			NpgsqlCommand cmd = new NpgsqlCommand("job_create", connection);

			cmd.CommandType = CommandType.StoredProcedure;

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "i_job_type",
				NpgsqlDbType = NpgsqlDbType.Smallint,
				Value = type
			});

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "c_new_job",
				NpgsqlDbType = NpgsqlDbType.Refcursor,
				Value = "job"
			});

			var reader = cmd.ExecuteReader();

			while(reader.Read())
				ret = reader.GetInt64(0);

			reader.Close();

			tx.Commit();

			connection.Close();

			return ret;
		}

		public void FinishJob(long Id)
		{
			var connection = GetConnection();

			var tx = connection.BeginTransaction();
			NpgsqlCommand cmd = new NpgsqlCommand("job_finish", connection);

			cmd.CommandType = CommandType.StoredProcedure;

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "i_job_id",
				NpgsqlDbType = NpgsqlDbType.Bigint,
				Value = Id
			});

			var reader = cmd.ExecuteNonQuery();

			tx.Commit();

			connection.Close();
		}

		public long GetUndownloadedListingCount(bool IncludeDeleted, string UrlFilter)
		{
			long ret = 0;

			var connection = GetConnection();

			var tx = connection.BeginTransaction();
			NpgsqlCommand cmd = new NpgsqlCommand("listing_get_undownloaded_count", connection);

			cmd.CommandType = CommandType.StoredProcedure;

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "include_deleted",
				NpgsqlDbType = NpgsqlDbType.Boolean,
				Value = IncludeDeleted
			});

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "url_filter",
				NpgsqlDbType = NpgsqlDbType.Varchar,
				Value = UrlFilter
			});

			var reader = cmd.ExecuteReader();

			while(reader.Read())
				ret = reader.GetInt64(0);

			reader.Close();

			tx.Commit();

			connection.Close();

			return ret;
		}

		public DataTable GetUndownloadedListings(bool IncludeDeleted, long PageSize, long PageNumber)
		{
			DataSet ret = new DataSet();

			var connection = GetConnection();

			var tx = connection.BeginTransaction();
			NpgsqlCommand cmd = new NpgsqlCommand("listing_get_undownloaded", connection);

			cmd.CommandType = CommandType.StoredProcedure;

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "include_deleted",
				NpgsqlDbType = NpgsqlDbType.Boolean,
				Value = IncludeDeleted
			});

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "c_listings",
				NpgsqlDbType = NpgsqlDbType.Refcursor,
				Value = "listings"
			});

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "page_size",
				NpgsqlDbType = NpgsqlDbType.Bigint,
				Value = PageSize
			});

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = "page_number",
				NpgsqlDbType = NpgsqlDbType.Bigint,
				Value = PageNumber
			});

			NpgsqlDataAdapter adapter = new NpgsqlDataAdapter(cmd);

			adapter.Fill(ret);

			tx.Commit();

			connection.Close();

			return ret.Tables[0];
		}

		public DataTable GetUndownloadedListingsWithLocation(bool IncludeDeleted, string UrlFilter, long PageSize, long PageNumber)
		{
			DataSet ret = new DataSet();

			var connection = GetConnection();

			//var tx = connection.BeginTransaction();

			StringBuilder sql = new StringBuilder("SELECT * from listing_undownloaded_with_location ");

			string clause = "WHERE";

			if(!IncludeDeleted)
			{
				sql.Append("WHERE deleted_on IS NULL ");
				clause = "AND";
			}

			sql.Append(clause + " url LIKE :url_filter ");

			sql.Append("OFFSET :page_number * :page_size LIMIT :page_size");

			NpgsqlCommand cmd = new NpgsqlCommand(sql.ToString(), connection);

			cmd.CommandType = CommandType.Text;

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = ":url_filter",
				NpgsqlDbType = NpgsqlDbType.Varchar,
				NpgsqlValue = '%' + UrlFilter + '%'
			});

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = ":page_number",
				NpgsqlDbType = NpgsqlDbType.Bigint,
				Value = PageSize
			});

			cmd.Parameters.Add(new NpgsqlParameter
			{
				Direction = ParameterDirection.Input,
				ParameterName = ":page_size",
				NpgsqlDbType = NpgsqlDbType.Bigint,
				Value = PageNumber
			});

			NpgsqlDataAdapter adapter = new NpgsqlDataAdapter(cmd);

			adapter.Fill(ret);

			//tx.Commit();

			connection.Close();

			return ret.Tables[0];
		}
	}
}

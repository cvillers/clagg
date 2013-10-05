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
				NpgsqlDbType = NpgsqlDbType.Smallint,
				Value = Id
			});

			var reader = cmd.ExecuteNonQuery();

			tx.Commit();

			connection.Close();
		}
	}
}

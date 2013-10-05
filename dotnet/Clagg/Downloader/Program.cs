using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using Npgsql;

namespace Downloader
{
	class Program
	{
		static void Main(string[] args)
		{
			NpgsqlConnection connection = new NpgsqlConnection(ConfigurationManager.ConnectionStrings["ClaggDB"].ConnectionString);

			connection.Open();
		}
	}
}

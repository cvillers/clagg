using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
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
			DbConnection db = new DbConnection();

			var id = db.CreateJob(JobTypes.Download);

			Console.WriteLine("New job id {0}", id);

			db.FinishJob(id);
		}
	}
}

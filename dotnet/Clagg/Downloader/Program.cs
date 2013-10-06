using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;

using Npgsql;

using OfficeOpenXml;

namespace Downloader
{
	class Program
	{
		private const int PageSize = 1000;

		private static Dictionary<string, string> Areas = new Dictionary<string, string>
		{
			{ "mnh", "Manhattan" },
			{ "brk", "Brooklyn" },
			{ "que", "Queens" }
		};

		static void Download(DbConnection db, string UrlFilter, ExcelWorksheet sheet)
		{
			long total = db.GetUndownloadedListingCount(false, UrlFilter);

			sheet.Cells["A1"].Value = "ID";
			sheet.Cells["B1"].Value = "Title";
			sheet.Cells["C1"].Value = "Price";
			sheet.Cells["C1"].StyleName = "Currency";
			sheet.Cells["D1"].Value = "URL";
			sheet.Cells["E1"].Value = "Latitude";
			sheet.Cells["F1"].Value = "Longitude";
			sheet.Cells["G1"].Value = "Address";

			int rc = 2;

			for(long page = 0; page <= total / PageSize; page++)
			{
				DataTable listings = db.GetUndownloadedListingsWithLocation(false, UrlFilter, PageSize, page);

				foreach(var row in listings.AsEnumerable())
				{
					sheet.Cells[rc, 1].Value = row["id"];
					sheet.Cells[rc, 2].Value = row["title"];
					sheet.Cells[rc, 3].Value = row["price"];
					sheet.Cells[rc, 4].Value = row["url"];
					sheet.Cells[rc, 5].Value = row["lat"];
					sheet.Cells[rc, 6].Value = row["long"];

					rc++;
				}
			}
		}

		static void Main(string[] args)
		{
			if(args.Length < 1)
			{
				Console.WriteLine("Usage: {0} <filename.xlsx>", Assembly.GetExecutingAssembly().GetName());

				return;
			}

			DbConnection db = new DbConnection();

			var id = db.CreateJob(JobTypes.Download);

			Console.WriteLine("New job id {0}", id);

			using(ExcelPackage package = new ExcelPackage(new FileInfo(args[0])))
			{
				foreach(var area in Areas)
				{
					ExcelWorksheet worksheet = package.Workbook.Worksheets.Add(area.Value);

					Download(db, area.Key, worksheet);
				}

				package.Save();
			}

			db.FinishJob(id);
		}
	}
}

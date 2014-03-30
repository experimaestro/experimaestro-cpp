// example.cpp : Simple logger example
//

#include "c11log/logger.h"
#include "c11log/sinks/async_sink.h"
#include "c11log/sinks/file_sinks.h"
#include "c11log/sinks/console_sinks.h"

#include "utils.h"

using std::cout;
using std::endl;
using namespace std::chrono;
using namespace c11log;
using namespace utils;


int main(int argc, char* argv[])
{

    const unsigned int howmany = argc <= 1 ? 1000000:atoi(argv[1]);

    logger cout_logger ("", sinks::stdout_sink());
    cout_logger.info() << "Hello " << "man";

    auto fsink = std::make_shared<sinks::rotating_file_sink>("log", "txt", 1024*1024*50 , 5, 0);
	auto fsink2 = std::make_shared<sinks::rotating_file_sink>("lllog", "txt", 1024*1024*50 , 5, 0);

    auto as = std::make_shared<sinks::async_sink>(1000);
	as->add_sink(sinks::null_sink::get());	

	logger my_logger ("my_logger", as);

    auto start = system_clock::now();
    for(unsigned int i = 1; i <= howmany ; ++i)
        my_logger.info() << "Hello logger: " << i;

	//as->shutdown(milliseconds(500));
	auto s = howmany - as->q().size();
    auto delta = system_clock::now() - start;
    auto delta_d = duration_cast<duration<double>> (delta).count();

    cout_logger.info() << "Total:" << format(s);
    cout_logger.info() << "Delta:" << format(delta_d);
    cout_logger.info() << "Rate:" << format(s/delta_d) << "/sec";

    return 0;
}

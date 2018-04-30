#include "process.hpp"

namespace TinyProcessLib
{
	Process::Process(const string_type &command, const string_type &path = string_type(),
		        OutputRedirect const & read_stdout = RedirectInherit(),
		        OutputRedirect const & read_stderr = RedirectInherit(),
		        InputRedirect const & write_stdin = RedirectInherit(),
		        size_t buffer_size = 131072) noexcept:
		closed(true), read_stdout(std::move(read_stdout)), read_stderr(std::move(read_stderr)), open_stdin(open_stdin), buffer_size(buffer_size)
	{
		open(command, path);
		async_read();
	}

	Process::~Process() noexcept
	{
		close_fds();
	}

	Process::id_type Process::get_id() const noexcept
	{
		return data.id;
	}

	bool Process::write(const std::string &data)
	{
		return write(data.c_str(), data.size());
	}
} // TinyProsessLib

#ifdef _WIN32
#include "process_win.cpp"
#else
#include "process_unix.cpp"
#endif

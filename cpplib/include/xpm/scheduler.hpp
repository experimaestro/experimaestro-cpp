/**
 * Scheduler and related classes (launcher, connector, etc.)
 */

#ifndef XPM_SCHEDULER_H
#define XPM_SCHEDULER_H

#include <string>

namespace xpm {

/// A command line job
class CommandLineJob {

};

class Scheduler {
public:
  /// Creates a new scheduler with a given path
  Scheduler(std::string const &path);
};

}

#endif
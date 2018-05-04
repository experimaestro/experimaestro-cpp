%nspace std::vector;
%nspace std::map;


%include <swiginterface.i>
%interface_impl(xpm::rpc::ServerObject);


namespace std {
    template<class T> class vector {
      public:
        typedef size_t size_type;
        typedef T value_type;
        typedef const value_type& const_reference;
        vector();
        vector(size_type n);
        int size() const;
        size_type capacity() const;
        void reserve(size_type n);
        %rename(isEmpty) empty;
        bool empty() const;
        void clear();
        %extend {
            bool add(T const &t) {
                $self->push_back(t);
                return true;
            }
            const_reference get(int i) throw (std::out_of_range) {
                int size = int(self->size());
                if (i>=0 && i<size)
                    return (*self)[i];
                else
                    throw std::out_of_range("vector index out of range");
            }
            value_type set(int i, const value_type& val) throw (std::out_of_range) {
                int size = int(self->size());
                auto previous = std::move((*self)[i]);
                if (i>=0 && i<size)
                    (*self)[i] = val;
                else
                    throw std::out_of_range("vector index out of range");

                // FIXME: should return value_type *
                return previous;
            }
        }
    };

}


%include "arrays_java.i";
%include "std_vector.i"

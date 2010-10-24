﻿/* 
 Copyright (c) 2010, Direct Project
 All rights reserved.

 Authors:
    Umesh Madan     umeshma@microsoft.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
Neither the name of the The Direct Project (nhindirect.org). nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
*/
using System;
using System.Net.Sockets;
using System.Xml.Serialization;

namespace Health.Direct.DnsResponder
{
    public class SocketServerSettings
    {
        public const short DefaultMaxConnectionBacklog = 64;
        public const short DefaultMaxActiveRequests = 64;
        public const short DefaultMaxOutstandingAccepts = 16;
        public const short DefaultReadBufferSize = 1024;
        
        short m_maxOutstandingAccepts = DefaultMaxOutstandingAccepts;
        short m_maxConnectionBacklog = DefaultMaxConnectionBacklog;
        short m_maxActiveRequests = DefaultMaxActiveRequests;
        short m_readBufferSize = DefaultReadBufferSize;
        
        public SocketServerSettings()
        {
        }
        
        /// <summary>
        /// Have these many asynchronous "Accept" or Receive calls already in place, so that actual request/connect acceptance 
        /// does not become a bottleneck
        /// </summary>
        [XmlElement]
        public short MaxOutstandingAccepts
        {
            get
            {
                return m_maxOutstandingAccepts;
            }
            set
            {
                if (value <= 0)
                {
                    throw new ArgumentException();
                }
                
                m_maxOutstandingAccepts = value;
            }
        }
        
        /// <summary>
        /// Connections not already accepted are Queued automatically by Winsock until they can be accepted by Winsock
        /// Set this to some reasonable limit between 20-200. Note: if you specify too high a number, .NET will automatically
        /// constrain the # based on OS restrictions. 
        /// </summary>
        [XmlElement]
        public short MaxConnectionBacklog
        {
            get
            {
                return m_maxConnectionBacklog;
            }
            set
            {
                if (value <= 0)
                {
                    throw new ArgumentException();
                }

                m_maxConnectionBacklog = value;
            }
        }

        /// <summary>
        /// Max requests you simultaneously want to handle. The socket server will automatically impose this limit
        /// </summary>
        [XmlElement]
        public short MaxActiveRequests
        {
            get
            {
                return m_maxActiveRequests;
            }
            set
            {
                if (value <= 0)
                {
                    throw new ArgumentException();
                }

                m_maxActiveRequests = value;
            }
        }
        
        [XmlElement]
        public short ReadBufferSize
        {
            get
            {
                return m_readBufferSize;
            }
            set
            {
                if (value <= 0)
                {
                    throw new ArgumentException();
                }
                
                m_readBufferSize = value;
            }
        }
        
        //--------------------------------------
        //
        // Self explanatory Socket Timeouts
        //
        //--------------------------------------
        
        [XmlElement]
        public int SendTimeout
        {
            get;
            set;
        }

        [XmlElement]
        public int ReceiveTimeout
        {
            get;
            set;
        }

        [XmlElement]
        public int SocketCloseTimeout
        {
            get;
            set;
        }
        
        [XmlIgnore]
        internal bool IsThrottled
        {
            get
            {
                return (m_maxActiveRequests > 0 && m_maxActiveRequests < short.MaxValue);
            }
        }
        
        //
        // Validation Etc
        //                                
        public virtual void Validate()
        {
            if (m_maxOutstandingAccepts <= 0)
            {
                throw new ArgumentException("MaxOutstandingAccepts");
            }
            
            if (m_maxConnectionBacklog <= 0)
            {
                throw new ArgumentException("MaxPendingConnections");
            }
            
            if (m_maxActiveRequests <= 0)
            {
                throw new ArgumentException("MaxActiveRequests");
            }
            
            if (m_readBufferSize <= 0)
            {
                throw new ArgumentException("ReadBufferSize");
            }
        }

        internal void ConfigureSocket(Socket socket)
        {
            if (this.ReceiveTimeout > 0)
            {
                socket.SetReceiveTimeout(this.ReceiveTimeout);
            }
            if (this.SendTimeout > 0)
            {
                socket.SetSendTimeout(this.SendTimeout);
            }
        }
        
        internal IWorkLoadThrottle CreateRequestThrottle()
        {
            if (this.IsThrottled)
            {
                return new WorkThrottle(m_maxActiveRequests);
            }
            
            return new NullThrottle();
        } 
        
        internal IWorkLoadThrottle CreateAcceptThrottle()
        {
            return new WorkThrottle(m_maxOutstandingAccepts);
        }
        
    }
}